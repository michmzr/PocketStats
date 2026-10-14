# Plan usunięcia integracji Pocket

Data: 2026-10-14. Gałąź: `codex/readwise-only-sync`.

Cel: usunąć logowanie i import z Pocket; umożliwiać synchronizację Readwise na podstawie obecności `READER_ACCESS_TOKEN`.

Wykonanie: w tej sesji, kolejno, po zatwierdzeniu planu. Nie publikować ani nie wdrażać zmian.

## Stan kodu

`TopAuth.vue` i `AuthorizationService` realizują logowanie Pocket. `SyncPanel.vue` korzysta ze wspólnego stanu autoryzacji Pocket także dla Readwise. Router sprawdza autoryzację Pocket i historię obu źródeł. `application.yaml` wymaga `POCKET_CONSUMER_KEY`. `ApiMigrationService` importuje oba źródła, a wariant bez argumentu uruchamia oba importy.

Backend: Java, Spring Boot 3.0.1, MongoDB, Spock. Frontend: Vue 3, TypeScript, Pinia. Repozytorium ma wrapper Gradle oraz zainstalowane zależności frontendowe. Dostępny jest pnpm.

## Granice zmian

Zachować wszystkie rekordy Pocket i ich historię synchronizacji. Zachować `Source.POCKET`, `Item`, `MigrationStatus`, repozytoria, modele wykorzystywane przez historyczne dane i mechanizm odczytu statystyk. Nie tworzyć migracji czyszczącej, nie uruchamiać operacji usuwania danych ani łączyć się z bazą produkcyjną podczas weryfikacji.

Nie usuwać całego pakietu `pocket`: zawiera modele używane przez bazę i statystyki. Usunąć wyłącznie kod aktywnej integracji. Nie zmieniać istniejących identyfikatorów ani oznaczenia źródła historycznych rekordów. Usunięcie integracji nie wymaga zmian schematu bazy.

## Kontrakt synchronizacji

Nowy `GET /sync/availability` zwraca standardowy `ApiResponse` z `data.available`: `true` wyłącznie dla niepustego `READER_ACCESS_TOKEN`, po pominięciu białych znaków. Odpowiedź nie zawiera tokena. Sprawdzenie dotyczy konfiguracji; odrzucony przez Readwise token pozostaje błędem synchronizacji.

`POST /sync/last` synchronizuje wyłącznie Readwise. `POST /sync/last/READER` zachowuje dotychczasowy format wyniku. `POST /sync/last/POCKET` zwraca HTTP 410 bez wywołania zewnętrznego API i bez zapisów do bazy. Brak tokena powoduje HTTP 503, również przed importem i zapisem historii.

`GET /sync/last/{source}` pozostaje dostępny dla obu źródeł, aby zachować odczyt historii Pocket. Usunąć `/pocket/auth/login`, `/pocket/auth/token` i `/pocket/auth/authorized`; nie zostawiać atrap logowania.

## Kolejność wykonania

### 1. Testy kontraktu i ochrony danych

- [x] Przepisać `src/test/groovy/eu/cybershu/pocketstats/migration/ApiMigrationServiceTest.groovy`, zastępując oczekiwania importu Pocket oczekiwaniami jego odrzucenia.
- [x] Dodać testy pełnego importu Readwise, importu od ostatniej synchronizacji, zachowania błędu importu i odczytu historii obu źródeł.
- [x] Dodać testy konfiguracji tokena: brak, pusty ciąg, same białe znaki, niepusty klucz. Wstrzykiwać konfigurację w testach, zamiast polegać na zmiennych środowiskowych maszyny.
- [x] Dodać `src/test/groovy/eu/cybershu/pocketstats/sync/SyncControllerTest.groovy` z testami odpowiedzi HTTP i formatu odpowiedzi, przy użyciu kontrolera i atrap usług, bez MongoDB.
- [x] W testach odrzucenia Pocket i braku tokena wymagać zerowych interakcji z usługą Readwise i repozytoriami; dla udanego importu dopuszczać jedynie oczekiwane zapisy rekordów i historii Readwise.

Przykładowa asercja odrzucenia na poziomie usługi:

```groovy
when:
migrationService.importAllFromSinceLastUpdate(Source.POCKET)

then:
def error = thrown(org.springframework.web.server.ResponseStatusException)
error.statusCode.value() == 410
0 * readerApiService._
0 * itemRepository._
0 * migrationStatusRepository._
0 * eventsPublisher._
```

Uruchomić testy przed implementacją i potwierdzić, że nowe oczekiwania zawodzą z powodu aktualnego zachowania.

### 2. Backend bez aktywnej integracji Pocket

- [x] Dodać `src/main/java/eu/cybershu/pocketstats/reader/ReaderSyncConfiguration.java`, pobierające `READER_ACCESS_TOKEN` z opcjonalnym domyślnym pustym ciągiem. Współdzielić sprawdzanie dostępności między synchronizacją i endpointem dostępności.
- [x] Zmienić konstruktor i implementację `ApiMigrationService.java`: usunąć zależność `PocketApiService`; odrzucać Pocket przed odczytem historii i zapisem; wariant bez źródła kierować do Readwise; zachować import pełny i przyrostowy Readwise.
- [x] W `SyncController.java` dodać endpoint dostępności oraz mały model odpowiedzi `SyncAvailability.java`. Zachować odczyt historycznego statusu Pocket.
- [x] Usunąć `auth/PocketApiController.java`, `auth/AuthorizationLink.java`, `auth/AuthorizationStatus.java`, `pocket/PocketAuthorizationService.java`, `pocket/PocketApiService.java`, `pocket/PocketUserCredentials.java` i `pocket/UserCurrentlyAuthorising.java`.
- [x] Usunąć niewykorzystywane importy usuniętych klas, w tym w `ApiMigrationUpdateDBService.java`. Pozostałe modele Pocket pozostawić.
- [x] Usunąć blok `auth.pocket` z `src/main/resources/application.yaml`.
- [x] Uruchomić testy usługi, konfiguracji i kontrolera. Sprawdzić, że aplikacja może utworzyć te komponenty bez zmiennych Pocket.

### 3. Frontend z jednym panelem Readwise

- [x] Usunąć `src/frontend/src/components/TopAuth.vue` i `src/frontend/src/services/authorization-service.ts`; usunąć rejestrację oraz użycie `TopAuth` z `App.vue`.
- [x] Usunąć stan sesji Pocket z `src/frontend/src/store/index.ts`, pozostawiając magazyn statusów synchronizacji.
- [x] Usunąć zapytania o autoryzację i status Pocket z `src/frontend/src/router/index.ts`.
- [x] W `HomeView.vue` pozostawić jeden `SyncPanel`, bez parametru pozwalającego wybrać Pocket.
- [x] W `sync-service.ts` kierować operacje do Readwise i udostępnić odczyt dostępności synchronizacji. Zwracać obietnice zapytań, żeby komponent mógł obsługiwać błędy.
- [x] W `SyncPanel.vue` sprawdzać dostępność klucza po zamontowaniu; pokazywać przycisk synchronizacji dla skonfigurowanego Readwise niezależnie od historii Pocket. Pobrać i wyświetlić status ostatniej synchronizacji także po ponownym zamontowaniu komponentu.
- [x] Po nieudanym zapytaniu zakończyć stan ładowania i wyświetlić komunikat błędu. Przy braku klucza pokazać niedostępność synchronizacji Readwise. Nie przekazywać tokena do przeglądarki.

### 4. Konfiguracja i dokumentacja

- [x] Usunąć wymaganie `POCKET_CONSUMER_KEY` i jego przykład z `README.md`; opisać opcjonalny `READER_ACCESS_TOKEN`, zachowanie bez klucza i zachowanie historycznych danych Pocket.
- [x] Z lokalnego, ignorowanego `env_dev.json` usunąć wyłącznie pole `POCKET_CONSUMER_KEY`, bez wypisywania pozostałych wartości. Jeśli istnieją inne pliki środowiskowe, sprawdzić tylko nazwy kluczy i usunąć konfigurację Pocket w ramach tego samego zakresu.
- [x] Kod nie będzie odczytywać ani zapisywać `pocket-creds.json`. Istniejący lokalny plik i regułę ignorowania pozostawić, aby nie usuwać niepotrzebnie danych użytkownika ani dopuścić przypadkowego dodania poświadczeń do Gita.

### 5. Weryfikacja i przegląd

- [x] Uruchomić izolowane testy: `rtk proxy ./gradlew test --tests '*ApiMigrationServiceTest' --tests '*ReaderSyncConfigurationTest' --tests '*SyncControllerTest'`.
- [x] Uruchomić istniejące testy modeli i mapowania Pocket, aby sprawdzić zgodność zachowanych modeli. Nie uruchamiać `ItemStatsServiceIntTest`, który wykonuje `deleteAll()`, ani `ReaderApiServiceTest`, który wywołuje prawdziwe API.
- [x] W katalogu `src/frontend` uruchomić `rtk pnpm exec vue-cli-service lint --no-fix` dla zmienionych plików oraz `rtk pnpm build`. Nie zmieniać menedżera pakietów ani pliku blokady w ramach tej zmiany.
- [x] Sprawdzić interfejs w przeglądarce z lokalnymi atrapami odpowiedzi: dostępny klucz, brak klucza, brak historii, udana synchronizacja, błąd synchronizacji. Sprawdzić, że nie występują zapytania `/pocket/auth/*` ani import Pocket.
- [x] Sprawdzić diff pod kątem zmian w modelach bazy, operacji usuwania danych i pozostałych aktywnych odwołań do konfiguracji Pocket. Statystyki historycznych danych mają zachować dotychczasowy mechanizm odczytu.
- [x] Zrobić przegląd zmian i przedstawić wykonane sprawdzenia oraz ewentualne blokady środowiskowe. Pozostawić kod na osobnej gałęzi bez merge i wdrożenia.

## Kryteria odbioru

Aplikacja działa bez konfiguracji Pocket. Interfejs nie oferuje logowania ani synchronizacji Pocket. Niepusty klucz Readwise umożliwia synchronizację; brak klucza blokuje ją przed wywołaniem API. Historyczne dane Pocket i ich modele pozostają zachowane. Synchronizacja zbiorcza nie uruchamia Pocket, a bezpośrednia próba importu Pocket zostaje odrzucona bez zapisów do bazy.

## Wykonanie i weryfikacja

Wykonano 2026-10-14 na gałęzi `codex/readwise-only-sync`.

Pierwszy test odrzucenia Pocket nie przeszedł przed zmianą: aktualna implementacja wykonywała operacje importu. Po zmianie przeszły 44 izolowane testy Spock, obejmujące synchronizację, konfigurację klucza, kontrakt HTTP, modele Pocket i mapowanie Readwise. Nie uruchamiano testu z `deleteAll()` ani testu prawdziwego API Readwise.

Frontend skompilowano, a lint zmienionych plików przeszedł. Wersja pnpm na tej maszynie automatycznie próbuje instalować zależności przed skryptami; weryfikację wykonano z `--config.verify-deps-before-run=false`, używając istniejących zależności, bez zmiany pliku blokady. Kompilator zgłasza ostrzeżenia o vue-property-decorator, rozmiarze pakietu i starszych danych Browserslist.

Testy w przeglądarce z lokalnymi atrapami API potwierdziły dostępność panelu z kluczem, brak historii, udaną synchronizację, komunikat błędu i ponowienie operacji, brak klucza przy zachowaniu statystyk, błąd sprawdzania dostępności i odczyt historii po ponownym otwarciu strony. Nie wystąpiły zapytania do endpointów Pocket. Końcowy przebieg nie zgłosił wyjątków JavaScript; celowo odrzucone żądania 500/503 były obsługiwane przez panel. Nie wywoływano zewnętrznych API.

Uzupełnienie zakresu wynikające z kontroli zależności: usunięto także blokady autoryzacji Pocket z komponentów statystyk i dodano ich początkowe ładowanie. Bez tego usunięcie magazynu sesji uniemożliwiałoby dostęp do historycznych danych. Zachowano odświeżanie po synchronizacji. Heatmapy rozpoczynają od stanu bez danych do wyświetlenia, żeby nie renderować wykresu przed pobraniem wyników.

Test przeglądarkowy wykrył brak reaktywności pól panelu inicjalizowanych `undefined`. Potwierdzono w kodzie zainstalowanej biblioteki vue-class-component, że pomija ona takie pola. Inicjalizacja `null` przywróciła odświeżanie statusu i komunikatów błędu.

Niezależny przegląd wykrył pozostałą konfigurację Pocket w `application-prod.yaml`; usunięto ją i nieaktualny opis zmiennej `BACKEND_URL`. Przegląd po poprawce nie wykazał istotnych problemów. Usunięto również niewykorzystywany wyjątek autoryzacji Pocket.

Porównanie z Gitem potwierdziło brak zmian w encjach, repozytoriach, źródłach danych i serwisie obliczającym statystyki. Lokalny klucz Pocket usunięto z ignorowanego `env_dev.json`; `pocket-creds.json` pozostawiono. Nie wykonano migracji bazy, merge ani wdrożenia. Rzeczywisty klucz Readwise nie był sprawdzany.

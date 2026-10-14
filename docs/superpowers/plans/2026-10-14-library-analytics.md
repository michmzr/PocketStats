# Statystyki biblioteki

Zakres zatwierdzony przez użytkownika: zaległości, wiek zaległości, czas do archiwizacji, porównanie okresów, kategorie i domeny. Praca na `codex/readwise-only-sync`, bez merge, publikacji, migracji ani kasowania danych Pocket.

## Definicje

Uwzględniać jedynie istniejące rekordy TO_READ i ARCHIVED. Pomijać DELETED oraz kategorie highlight/note. Brak kategorii oznaczać jako unknown. Domyślnie oba źródła; filtr źródła ALL/READER/POCKET dostępny w interfejsie. Filtr dotyczy wszystkich sześciu statystyk.

Zaległości: liczba TO_READ. Wiek: pełne dni od timeAdded do czasu raportu, przedziały 0–30, 31–90, 91–180 i 181+; brak daty lub data przyszła trafia do unknownAge. Nie pomijać tych rekordów w backlog.total.

Czas do archiwizacji: mediana liczby dni pomiędzy timeAdded a timeRead dla ARCHIVED. Wykluczyć brakujące daty, ujemne różnice i daty archiwizacji w przyszłości; zwrócić liczbę uwzględnionych i pominiętych rekordów. Dla pustej próby medianDays=null. W Reader timeRead jest mapowane z last_moved_at dla archive; to data ostatniego przeniesienia do archiwum, nie pomiar czasu czytania.

Porównanie: dni=7/30/90, domyślnie 30. Przy stałym asOf aktualny okres [asOf-days, asOf), poprzedni [asOf-2*days, asOf-days). Added liczyć z timeAdded niezależnie od aktualnego statusu TO_READ/ARCHIVED, Read tylko dla ARCHIVED z timeRead. Deltę procentową przy poprzednim wyniku 0 zwracać jako null, bez Infinity/NaN.

Kategorie: ranking z count, backlog, archived. Domeny: top 10 hostów z Item.url, prawidłowe URL http/https oraz domeny nadawców z mailto (pierwszy adres przed przecinkiem, bez parametrów); małe litery i usunięty prefiks www. Łączyć hosty z różnymi ścieżkami i portami, nie łączyć pozostałych subdomen. Brakujące/niepoprawne adresy zliczać osobno jako unknownDomainCount. Kolejność malejąco po count, przy remisie alfabetycznie.

## Kontrakt

GET /stats/library?days=30&source=READER. Source opcjonalne, pominięte oznacza wszystkie źródła. Standardowe ApiResponse. days inne niż 7/30/90 oraz nieznane źródło: HTTP 400. Endpoint wyłącznie odczytowy i niezależny od konfiguracji klucza Readwise.

```json
{
  "status": 0,
  "message": "ok",
  "data": {
    "asOf": "2026-10-14T12:00:00Z",
    "source": null,
    "backlog": {
      "total": 12,
      "ageBuckets": [
        {"key":"0-30","label":"0–30 days","count":4},
        {"key":"31-90","label":"31–90 days","count":3},
        {"key":"91-180","label":"91–180 days","count":2},
        {"key":"181+","label":"181+ days","count":2}
      ],
      "unknownAge": 1
    },
    "archiveDelay": {"medianDays": 8.5,"eligibleRecords":10,"excludedRecords":2},
    "comparison": {
      "days":30,
      "current":{"start":"2026-09-14T12:00:00Z","end":"2026-10-14T12:00:00Z","added":8,"read":4},
      "previous":{"start":"2026-08-15T12:00:00Z","end":"2026-09-14T12:00:00Z","added":5,"read":0},
      "added":{"delta":3,"percent":60.0},
      "read":{"delta":4,"percent":null}
    },
    "categories":[{"name":"article","count":24,"backlog":12,"archived":12}],
    "domains":[{"name":"example.org","count":23,"backlog":11,"archived":12}],
    "unknownDomainCount":1
  }
}
```

## Podział pracy

- [x] Subagent danych: audyt response i mapowania; bez rozszerzania importu, jeżeli istniejące pola dokumentów wystarczają. Nie kopiować danych prywatnych do repozytorium.
- [x] Subagent backendu: nowe modele, kalkulator, odczyt ze strumienia projekcji MongoDB, kontroler i izolowane testy. Bez pobierania całej kolekcji do listy i bez zapisów do bazy. Mediana wymaga przechowania wyłącznie wartości czasu, nie całych dokumentów.
- [x] Subagent frontendu: modele i klient endpointu, komponent z sześcioma sekcjami, filtry source/days, loading/error/retry, stany puste i dostępność klawiaturą; wpięcie w stronę główną. Używać istniejącego Vue/Bootstrap.
- [x] Agent główny: kontrakt, dokumentacja API, integracja, przegląd oraz testy przeglądarkowe z kontrolowanymi odpowiedziami. Potwierdzić wszystkie liczby, filtry, stany błędów i brak wpływu na dane Pocket.

## Weryfikacja

Testy obliczeń z zamrożonym Clock: granice wieku, brakujące/future daty, mediany parzyste i nieparzyste, pusta biblioteka, źródła, DELETED/highlight/note, granice okresów, poprzednia wartość zero, normalizacja domen i ranking. Testy kontrolera: kontrakt JSON, days/source validation, odczyt bez tokena. Testy serwisu: zamykanie strumienia i brak zapisów.

Kompilacja i lint frontendu, testy strony w przeglądarce na desktop/mobile. Nie uruchamiać testów prawdziwego Readwise ani testu MongoDB z deleteAll. Nie importować załącznika do bazy. Używać Java 17 przez istniejący wrapper Gradle oraz pnpm z wyłączoną automatyczną instalacją przed skryptami.

## Wykonanie

Zrealizowano z podziałem na subagentów audytu danych, backendu i frontendu, a następnie niezależny przegląd. Audyt wskazał 68 dokumentów i 32 wyróżnienia w próbce 100 wyników. Wszystkie dokumenty miały source_url. 29 dokumentów newsletterów używało mailto, co uwzględniono w rankingu domen. Nie zmieniono importera; dodano syntetyczny test istniejącego mapowania. Załącznika nie skopiowano do repozytorium ani do bazy.

Przeszły 64 izolowane testy Spock, w tym 19 nowych testów analityki. Kompilacja i lint frontendu przeszły; pozostały ostrzeżenia narzędzi o starszych danych Browserslist, dekoratorach Vue i rozmiarze pakietu. Nowe testy przeglądarkowe z atrapami API objęły osiem obszarów: sześć statystyk, ładowanie i blokady filtrów, brak klucza, filtry źródła/okresu, puste dane, błąd i Retry, odświeżanie po synchronizacji oraz widok mobilny. Istniejący test Added/Read przeszedł wszystkie siedem scenariuszy po dodaniu nowej sekcji. Końcowe testy nie zgłosiły wyjątków JavaScript.

Niezależny przegląd nie znalazł istotnych problemów. Udokumentowano endpoint w lokalnym OpenAPI. Nie uruchamiano prawdziwego Readwise ani testu MongoDB z deleteAll. Nowy serwis wyłącznie czyta projekcję w zamykanym strumieniu; nie wykonuje zapisów ani zmian schematu. Nie wykonano merge ani wdrożenia.

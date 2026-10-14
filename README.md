# PocketStats

App for visualising historical Pocket data and Readwise Reader usage stats.

Pocket login and synchronization are no longer available. Existing Pocket items, migration history and statistics are retained. Synchronization imports only Readwise Reader, including when no source is specified.

## Config

### Backend

#### Environment variables
- `READER_ACCESS_TOKEN` - optional Readwise access key; synchronization is available only when the key is nonblank. Without it, existing statistics remain accessible.
- `MONGODB_HOST` - mongo db host 
- `MONGODB_URI` - full mongodb uri with login and pass

example env variables for local instance workig with dockerized MongoDB
```
{
  "VUE_APP_BACKEND_URL": "http://127.0.0.1:8081/pocketstats",
  "READER_ACCESS_TOKEN": "[Readwise access key]",
  "MONGODB_DB": "test",
  "MONGODB_URI": "mongodb+srv://..."
}
```

#### Database
Docker service file with mongodb can be find here: [mongodb.yml](https://github.com/michmzr/PocketStats/blob/master/src/main/docker/mongodb.yml)

### Frontend 
#### Environment variables
- `VUE_APP_BACKEND_URL` - java backend url

**You can read more in
article**: [https://cybershu.eu/articles/pocket-stats-analytics-app-get-pocket-api.html](https://cybershu.eu/articles/pocket-stats-analytics-app-get-pocket-api.html) (it describes old version!!)

## Synchronization API

`GET /sync/availability` reports `data.available` without exposing the token.
`POST /sync/last` and `POST /sync/last/READER` synchronize Readwise.
Without a configured key these calls return HTTP 503. A configured key rejected
by Readwise causes a synchronization error.
`POST /sync/last/POCKET` returns HTTP 410 without modifying stored data.
`GET /sync/last/POCKET` continues to return historical synchronization status.
Pocket authentication endpoints have been removed.

## Library statistics

The homepage includes backlog count and age, median time to archive, consecutive
period comparison, category ranking and the top 10 domains. The source selector
includes both retained sources by default and can restrict the panel to Readwise
or historical Pocket records. These statistics remain available without a
Readwise key. Highlights, notes and deleted items are excluded.

Backlog age uses full days since addition, split into 0–30, 31–90, 91–180 and
181+ days. Missing or future addition dates have unknown age. Archive delay uses
valid addition and archive timestamps; the panel displays its sample size and
the number of omitted records. The imported Reader archive timestamp comes from
last_moved_at for an archived document. It describes elapsed time to archival.

Comparison uses rolling 7, 30 or 90 day intervals ending at the snapshot time.
The previous interval has the same length. A zero previous count has no
percentage baseline. Selecting a period affects only this comparison;
backlog, age, categories and domains remain snapshots of all retained records
in the selected source. Newsletter mailto sender domains are included alongside
HTTP/HTTPS hosts. Missing or invalid addresses are counted separately.

`GET /stats/library?days=30&source=READER` returns these statistics. Omit source
to include both sources. Unsupported days or source return HTTP 400.
The local contract is in [library-analytics.openapi.json](docs/api/library-analytics.openapi.json).
The endpoint streams a read-only field projection, with no data migration or writes.
It retains only delay values for the exact median and counters for rankings.

The attached response was used as reference, not imported into the database.
Reader's list response includes child highlights and notes as documents.[^reader-api]

[^reader-api]: [Readwise Reader API documentation](https://readwise.io/reader_api).

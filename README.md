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

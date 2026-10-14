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

## CI and Coolify deployment

GitHub Actions checks pushes, pull requests and manual runs with Java 17, the
Gradle wrapper and Node 22. Frontend dependencies use `npm ci` with the existing
`package-lock.json`; the Gradle frontend install task is excluded to preserve that
locked install. Checks include frontend lint and types, backend build/tests,
JaCoCo coverage and the offline private-dashboard launcher/Compose tests.

`.github/ci.gradle` disables embedded MongoDB and points Spring tests at the
disposable MongoDB 8 service, isolated from Atlas. The live
`ReaderApiServiceTest` requires a real Readwise account and is excluded from
secret-free CI; run it separately with a dedicated test-account token. No
application credentials are required by the standard CI job. Only test and
coverage reports are uploaded; the application JAR is not published.

Optional SonarCloud analysis runs only after successful checks on pushes to
`master`, when repository variable `SONAR_ENABLED=true` and repository secret
`SONAR_TOKEN` are configured. It is not run for pull requests or manual runs.

Coolify owns deployment. Actions does not copy a JAR over SSH, restart systemd,
call a deployment webhook or probe a production URL. The private Compose
profile uses separately staged protected host artifacts and runtime-only
configuration managed by Coolify. A successful source build does not replace
the deployed JAR or establish release admission. Changes to the active
Coolify branch, pinned artifacts or deployment profile require their own
review and runtime checks. See [the Compose profile](deploy/coolify/README.md).

### Current source image profile

`deploy/coolify/compose-source.yaml` is the replacement profile for the existing
private dashboard. It reads native Coolify runtime variables and runs matched
backend/UI images built from one master commit. The legacy JAR, launcher,
Nginx configuration and static UI are no longer host-mounted in this profile;
the protected dashboard marker and existing writable state mounts remain.
New library analytics are available through the read-only proxy. The private
profile reports synchronization unavailable and continues to deny writes.

The manual `Build PocketStats source images` workflow builds both images on
GitHub, verifies their runtime configuration and scans complete images.
Fixable HIGH/CRITICAL vulnerabilities and secret matches block publication by default.
The owner-approved private deployment exception can be explicitly selected
for this manual run until 2026-10-21; secrets still block and scan evidence is retained.
Only admitted images are pushed with immutable `sha-<commit>` tags; no moving
release channel is promoted. Image digests and sanitized scan/SBOM evidence
are attached to the run. Select these digests as `POCKETSTATS_RUNTIME_IMAGE`
and `POCKETSTATS_PROXY_IMAGE`, pin the tested Git revision and select
`/compose-source.yaml` in Coolify only after admission. Retain the previous
profile, pinned revision and images for rollback. A successful source build
alone does not authorize replacing the live pair.

### Automatic releases and Readwise synchronization

Successful trusted master-push CI builds matched source images from its exact
commit and promotes both `coolify-dev` channels. A host timer checks every two
minutes, accepts only matching revisions with the admitted deployment-contract
fingerprint, preserves stopped/busy applications and selects immutable digests
through Coolify. Failed, PR and stale runs do not promote; GitHub receives no
server credentials. The owner exception covers HIGH/CRITICAL through2026-10-21;
scans remain recorded and secret matches always block.

Readwise synchronization is enabled by default when its runtime token is
configured. The private UI loads real availability and history, and the Sync
Readwise button imports on demand. Deployment and the release timer never start
an import. The private proxy allows GET availability, GET/POST `/sync/last/READER`
and default POST `/sync/last`. Write requests require `X-PocketStats-Sync: 1`,
which the UI sends; cross-site form writes and preflight requests are denied.
Retired Pocket authentication/synchronization remains unavailable.

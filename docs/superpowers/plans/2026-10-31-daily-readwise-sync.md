# Daily Readwise synchronization

The owner approved server-side synchronization every day at 18:00 Polish time.
Use a host systemd timer with `OnCalendar=*-*-* 18:00:00 Europe/Warsaw`,
one-second accuracy and persistent missed-run handling. This uses the existing
loopback-only proxy and API, without new credentials, container changes or a
dependency on the owner's computer. The oneshot service prevents overlapping
timer instances. It checks availability, submits one incremental Readwise import,
validates application-level success and logs the result. Do not automatically
retry POST requests after an ambiguous timeout.

Implementation and verification:

- Add regression tests for availability gating, successful imports, unsuccessful
  application responses, invalid JSON and transport failures. Confirm they fail
  before the runner exists.
- Add `deploy/coolify/readwise-sync.py`, `pocketstats-readwise-sync.service` and
  `pocketstats-readwise-sync.timer`. Run the tests and systemd unit validation.
- Install the runner and units on the existing `dev-server`, start one real
  synchronization through the service, and verify updated API history.
- Enable the timer, inspect the next trigger, and verify both winter and summer
  offsets using `systemd-analyze calendar`.
- Document installation, logs and rollback. Review the diff and commit the
  deployment artifacts. No application image release is needed.

Verified on 2026-10-31: six runner tests passed; systemd accepted both units.
The service completed with exit status 0 and imported 14 records. The API
history confirms success at `2026-10-31T08:49:58.553Z`. The timer is enabled and
active, with its next trigger at `2026-10-31 17:00:00 UTC` (18:00 Warsaw).
Calendar checks across 2027-03-28 and 2027-10-31 confirmed the switch between
16:00 and 17:00 UTC while retaining 18:00 Polish time. Reviewed the runner,
units and documentation together; application/API code is unchanged.

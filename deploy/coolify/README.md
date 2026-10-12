---
tags: [dev, resource, pocketstats, coolify]
aliases: []
created: 2026-10-02
updated: 2026-10-12
status: prepared
---

# Private Coolify statistics dashboard images

Local security verification currently blocks publication: the fresh-source backend image has fixed HIGH/CRITICAL dependency findings, including `CVE-2026-47884` in Spring MVC6.0.3 (reported fix7.0.9) and `CVE-2022-1471` in SnakeYAML1.33 (reported fix2.0). Those major changes require separate acceptance. The pinned proxy base also has fixed HIGH/CRITICAL package findings, including Nginx1.28.2 requiring1.28.3 fixes. No imported JAR was scanned or published; zero embedded secret findings were reported in either fresh-source image.

The GHCR candidate builds a fresh Java17 JAR and complete Vue dashboard from source. It never reads or publishes the imported host JAR. The packaged MongoDB URI and Pocket consumer key bind from runtime environment. The publication guard checks packaged application resources without printing credentials. Reader access remains supplied through `READER_ACCESS_TOKEN` at runtime.

`coolify-images.yml` publishes only trusted pushes to `master` or `feat/coolify-ghcr-publishing`, with `GITHUB_TOKEN` contents-read/packages-write permissions on a GitHub-hosted runner. It publishes `ghcr.io/michmzr/pocketstats` and `ghcr.io/michmzr/pocketstats-dashboard` under `coolify-dev` and `sha-<full-commit>` tags with OCI source/revision labels. Trivy source credential/config checks and fixed HIGH/CRITICAL image vulnerability/secret gates block promotion. Global concurrency serializes channel updates. Both scanned immutable candidates must be pushed and their revision labels verified before either channel moves. CycloneDX SBOM artifacts record each exact pushed digest; raw secret scan reports are never uploaded. Immutable deployment references use the resulting manifest digests. The older source-host build/deployment job runs only on main/master pushes; its unrelated test behavior is retained.

Set native `POCKETSTATS_RUNTIME_IMAGE` and `POCKETSTATS_PROXY_IMAGE` to the accepted GHCR digests and retain native Custom Build Command `true`, empty Custom Start Command and runtime-only backend environment delivery. Compose pulls these images with `pull_policy: always`. The old `/usr/lib/pocketstats/app.jar` bind and imported UI-release bind are removed so both current JAR and frontend code are consumed from the images. Host copies remain available for rollback. Existing launcher, protected marker and private Nginx configuration bind mounts remain Git/native inputs.

`MONGODB_URI`, `MONGODB_DB`, `POCKET_CONSUMER_KEY` and `READER_ACCESS_TOKEN` remain native Runtime/Literal values with Buildtime disabled. No real values belong in Git, build inputs or logs. The unchanged backend launcher requires database `test`, matching URI, root:9820640 version-2 shared-dashboard marker, production profile and index-disable options. It strips Spring JSON and Java-option injection.

The frontend reads `window.POCKETSTATS_CONFIG.backendUrl` before requests. `dashboard-start` validates public `VUE_APP_BACKEND_URL` and emits only that value under bounded `/tmp` tmpfs; the HTML loads `/runtime-config.js`. Empty means same-origin statistics routes. HTTP(S) and root-relative bases without query, fragment, quote, backslash or newline are supported; a trailing slash is normalized. Source frontend assets replace the former fixed generated app filename/hash; new code updates are therefore consumed on each image deployment.

Compose retains UID999:GID982 backend and UID101:GID101 dashboard, protected state/home/marker mounts, read-only roots, dropped capabilities, resource limits and dedicated networks. Only the proxy publishes `127.0.0.1:18081`. Five exact statistics routes/methods remain; sync/auth/unmatched routes are denied. Native custom build stays `true`; deployment preparation does not change Atlas access or source-host services.

Run `test-launcher.py` and `test-config.py` for guard/config checks, then `test-dashboard-runtime.py <built-proxy-image>` on a trusted Docker host for actual nonroot/read-only runtime editing and injection rejection. `check-packaged-config.py` validates the newly built source JAR. Publication executes these gates before pushing. Deployment still requires target image pulls, protected marker access, bounded functional statistics requests, forbidden-route denial and host firewall verification. Offline checks and publication alone are not live deployment evidence.

The `ps-ui-ingress` noninternal bridge retains loopback publication and requires existing Ansible-owned INPUT/FORWARD denial of NEW outbound traffic. Preserve native Max restart count `3` and deliberate-stop semantics. `unless-stopped` does not itself enforce a hard three-retry limit. Preview deployments remain disabled because the bridge name permits one active instance.

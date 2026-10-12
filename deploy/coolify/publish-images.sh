#!/bin/bash
set -euo pipefail
: "${GITHUB_SHA:?}"
mkdir -p security-reports
for image in pocketstats pocketstats-dashboard; do
  test "$(docker inspect --format '{{ index .Config.Labels "org.opencontainers.image.revision" }}' "$image:verify")" = "$GITHUB_SHA"
  candidate="ghcr.io/michmzr/$image:sha-$GITHUB_SHA"
  docker tag "$image:verify" "$candidate"
  docker push "$candidate"
  digest=$(docker inspect --format '{{ index .RepoDigests 0 }}' "$candidate")
  case "$digest" in "ghcr.io/michmzr/$image@sha256:"*) ;; *) exit 1 ;; esac
  docker pull "$digest"
  test "$(docker inspect --format '{{ index .Config.Labels "org.opencontainers.image.revision" }}' "$digest")" = "$GITHUB_SHA"
  printf '%s\n' "$digest" > "security-reports/$image.digest"
  python3 deploy/coolify/bind-sbom.py "security-reports/$image.sbom.json" "$digest"
  docker buildx imagetools inspect "$digest" > "security-reports/$image.manifest.txt"
done
# Both candidates were scanned, pushed and verified before either channel moves.
for image in pocketstats pocketstats-dashboard; do
  digest=$(cat "security-reports/$image.digest")
  docker buildx imagetools create --tag "ghcr.io/michmzr/$image:coolify-dev" "$digest"
  echo "$digest (revision $GITHUB_SHA)" >> "$GITHUB_STEP_SUMMARY"
done

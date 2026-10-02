#!/bin/bash
# Export only verified runtime/proxy images, no application artifact or secrets.
set -euo pipefail
task_export_dir=/private/tmp/pocketstats-coolify-runtime-20261002
mkdir -p "$task_export_dir"
native_export_dir=$(mktemp -d /var/tmp/pocketstats-image-export.XXXXXX)
trap 'rm -rf "$native_export_dir"' EXIT
export TMPDIR="$native_export_dir"
export_image() {
  name=$1 image=$2
  test ! -e "$task_export_dir/$name.tar"
  podman save --format=docker-archive --output="$native_export_dir/$name.tar" "$image"
  cp "$native_export_dir/$name.tar" "$task_export_dir/$name.tar"
  sha256sum "$task_export_dir/$name.tar"
  stat -c '%n %s bytes' "$task_export_dir/$name.tar"
  rm "$native_export_dir/$name.tar"
}
export_image pocketstats-runtime localhost/pocketstats-private-runtime:20261002
export_image pocketstats-nginx docker.io/library/nginx@sha256:8f84ed99befc3891b8f329c5c202785278a2cfb7c25107d57fb2a134a3117433

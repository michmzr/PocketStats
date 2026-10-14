#!/bin/bash
set -euo pipefail
version=0.75.0
archive="trivy_${version}_Linux-64bit.tar.gz"
root="https://github.com/aquasecurity/trivy/releases/download/v${version}"
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
curl -fsSL "$root/$archive" -o "$work/$archive"
curl -fsSL "$root/trivy_${version}_checksums.txt" -o "$work/checksums.txt"
(cd "$work"; grep " $archive$" checksums.txt | sha256sum -c -)
tar -xf "$work/$archive" -C "$work" trivy
mkdir -p "$HOME/.local/bin"
install -m 0755 "$work/trivy" "$HOME/.local/bin/trivy"
echo "$HOME/.local/bin" >> "$GITHUB_PATH"

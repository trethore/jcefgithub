#!/usr/bin/env bash
set -Eeuo pipefail

[[ $# -eq 2 ]] || { echo "Usage: $0 <build_meta_url> <mvn_version>" >&2; exit 2; }
build_meta_url=$1
mvn_version=$2
metadata_file=$(mktemp)
curl --fail --show-error --silent --location --proto '=https' --tlsv1.2 -o "$metadata_file" "$build_meta_url"

required=(release_tag release_url jcef_url jcef_repository jcef_commit jcef_commit_long cef_version \
  download_url_linux_amd64 download_url_linux_arm64 download_url_windows_amd64 download_url_windows_arm64 \
  download_url_macosx_amd64 download_url_macosx_arm64)
for key in "${required[@]}"; do
  value=$(jq -er --arg key "$key" '.[$key] | select(type == "string" and length > 0)' "$metadata_file") || {
    echo "Missing or invalid build metadata field: $key" >&2; exit 1;
  }
  printf -v "$key" '%s' "$value"
  export "${key?}"
done
export mvn_version
rm -f "$metadata_file"

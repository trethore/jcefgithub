#!/usr/bin/env bash
# shellcheck disable=SC2154
set -Eeuo pipefail

if [ ! $# -eq 2 ]
  then
    echo "Usage: ./generate_maven_builds.sh <build_meta_url> <mvn_version>"
    echo ""
    echo "build_meta_url: The url to download build_meta.json from"
    echo "mvn_version: The maven version to export to"
    exit 1
fi

#CD to dir of this script
cd "$( dirname "$0" )"

#Set build info
# shellcheck source=packaging/scripts/set_build_info.sh
. ./set_build_info.sh "$1" "$2"

#Clear export dir
rm -rf /jcefout/*

echo "Creating maven artifacts for $mvn_version - $release_tag..."

maven_central_base_url="${MAVEN_CENTRAL_BASE_URL:-https://repo1.maven.org/maven2}"
group_path="io/github/trethore"

artifact_exists_on_maven_central() {
  local artifact_id="$1"
  local version="$2"
  local pom_url="$maven_central_base_url/$group_path/$artifact_id/$version/$artifact_id-$version.pom"
  local http_code

  http_code=$(curl --silent --show-error --location --output /dev/null --write-out '%{http_code}' \
    --proto '=https' --tlsv1.2 "$pom_url") || {
      echo "Could not check Maven Central for $artifact_id:$version" >&2
      exit 1
    }

  case "$http_code" in
    200) return 0 ;;
    404) return 1 ;;
    *)
      echo "Maven Central returned HTTP $http_code for $artifact_id:$version" >&2
      exit 1
      ;;
  esac
}

reuse_maven_central_artifact() {
  local artifact_id="$1"
  local version="$2"
  local base_url="$maven_central_base_url/$group_path/$artifact_id/$version/$artifact_id-$version"
  local temp_dir
  local suffix
  local remote_sha256
  local local_sha256

  echo "Reusing $artifact_id:$version from Maven Central..."
  temp_dir=$(mktemp -d)
  for suffix in .jar .pom -sources.jar -javadoc.jar; do
    curl --fail --show-error --silent --location --retry 3 \
      --proto '=https' --tlsv1.2 \
      --output "$temp_dir/$artifact_id-$version$suffix" "$base_url$suffix"
  done

  remote_sha256=$(curl --fail --show-error --silent --location --retry 3 \
    --proto '=https' --tlsv1.2 "$base_url.jar.sha256" | tr -d '[:space:]')
  local_sha256=$(sha256sum "$temp_dir/$artifact_id-$version.jar" | cut -d' ' -f1)
  if [ "$remote_sha256" != "$local_sha256" ]; then
    echo "SHA-256 verification failed while reusing $artifact_id:$version" >&2
    rm -rf "$temp_dir"
    exit 1
  fi

  mv "$temp_dir"/* /jcefout/
  rmdir "$temp_dir"
}

#API
echo "###########################################"
echo "# Creating JCEF API for all platforms     #"
echo "###########################################"
if artifact_exists_on_maven_central "jcef-api" "$release_tag"; then
  reuse_maven_central_artifact "jcef-api" "$release_tag"
else
  ./generate_jcef_api.sh
fi

platforms=(linux-amd64 linux-arm64 macosx-amd64 macosx-arm64 windows-amd64 windows-arm64)
layouts=(linux64 linux64 macos64 macos64 win64 win64)
urls=(
  "$download_url_linux_amd64" "$download_url_linux_arm64"
  "$download_url_macosx_amd64" "$download_url_macosx_arm64"
  "$download_url_windows_amd64" "$download_url_windows_arm64"
)

for i in "${!platforms[@]}"; do
  echo "###########################################"
  echo "# Creating native build for ${platforms[$i]}"
  echo "###########################################"
  artifact_id="jcef-natives-${platforms[$i]}"
  if artifact_exists_on_maven_central "$artifact_id" "$release_tag"; then
    reuse_maven_central_artifact "$artifact_id" "$release_tag"
  else
    ./generate_natives.sh "${layouts[$i]}" "${platforms[$i]}" "$release_tag" "${urls[$i]}"
  fi
done

# jcefgithub is built last so its metadata can include native artifact hashes.
echo "###########################################"
echo "# Creating jcefgithub for all platforms   #"
echo "###########################################"
./generate_jcefgithub.sh

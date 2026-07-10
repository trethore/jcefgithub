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

#API
echo "###########################################"
echo "# Creating JCEF API for all platforms     #"
echo "###########################################"
./generate_jcef_api.sh

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
  ./generate_natives.sh "${layouts[$i]}" "${platforms[$i]}" "$release_tag" "${urls[$i]}"
done

# jcefgithub is built last so its metadata can include native artifact hashes.
echo "###########################################"
echo "# Creating jcefgithub for all platforms   #"
echo "###########################################"
./generate_jcefgithub.sh

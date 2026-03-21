#!/bin/bash
set -e

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
. set_build_info.sh $1 $2

#Clear export dir
rm -rf /jcefout/*

echo "Creating maven artifacts for $mvn_version - $release_tag..."

#API
echo "###########################################"
echo "# Creating JCEF API for all platforms     #"
echo "###########################################"
./generate_jcef_api.sh

#jcefgithub
echo "###########################################"
echo "# Creating jcefgithub for all platforms    #"
echo "###########################################"
./generate_jcefgithub.sh

#Linux amd64
echo "###########################################"
echo "# Creating native build for linux-amd64   #"
echo "###########################################"
./generate_natives.sh linux64 linux-amd64 $release_tag $download_url_linux_amd64

#Linux arm64
echo "###########################################"
echo "# Creating native build for linux-arm64   #"
echo "###########################################"
./generate_natives.sh linux64 linux-arm64 $release_tag $download_url_linux_arm64

#Macos amd64
echo "###########################################"
echo "# Creating native build for macosx-amd64  #"
echo "###########################################"
./generate_natives.sh macos64 macosx-amd64 $release_tag $download_url_macosx_amd64

#Macos arm64
echo "###########################################"
echo "# Creating native build for macosx-arm64  #"
echo "###########################################"
./generate_natives.sh macos64 macosx-arm64 $release_tag $download_url_macosx_arm64

#Windows amd64
echo "###########################################"
echo "# Creating native build for windows-amd64 #"
echo "###########################################"
./generate_natives.sh win64 windows-amd64 $release_tag $download_url_windows_amd64

#Windows arm64
echo "###########################################"
echo "# Creating native build for windows-arm64 #"
echo "###########################################"
./generate_natives.sh win64 windows-arm64 $release_tag $download_url_windows_arm64

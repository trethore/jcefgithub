#!/bin/bash
set -e

if [ ! $# -eq 2 ]
  then
    echo "Usage: ./set_build_info.sh <build_meta_url> <mvn_version>"
    echo ""
    echo "build_meta_url: The url to download build_meta.json from"
    echo "mvn_version: The maven version to export to"
    exit 1
fi

#Print build meta location
echo "Initializing for build from $1 for $2..."

#Download build_meta.json and import to local environment
export $(curl -s -L $1 | jq -r "to_entries|map(\"\(.key)=\(.value|tostring)\")|.[]")

#Set jcefgithub information
export mvn_version=$2

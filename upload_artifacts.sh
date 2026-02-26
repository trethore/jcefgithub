#!/bin/bash
set -e

if [ ! $# -eq 2 ]; then
  echo "Usage: ./upload_artifacts.sh <build_meta_url> <mvn_version>"
  exit 1
fi

# CD to dir of this script
cd "$( dirname "$0" )"

# Set build info
. scripts/set_build_info.sh "$1" "$2"

# Move artifacts to a non-protected folder
rm -rf upload
mkdir upload
cp out/* upload/

echo "Deploying artifacts to GitHub Packages for $mvn_version..."

# Configure Maven settings for GitHub Packages authentication
mkdir -p ~/.m2
cat > ~/.m2/settings.xml <<EOF
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>\${env.GITHUB_ACTOR}</username>
      <password>\${env.GITHUB_TOKEN}</password>
    </server>
  </servers>
</settings>
EOF

chmod +x upload_artifact.sh

# Upload API
./upload_artifact.sh io.github.trethore jcef-api "$release_tag"

# Upload jcefgithub
./upload_artifact.sh io.github.trethore jcefgithub "$mvn_version"

# Upload linux natives
./upload_artifact.sh io.github.trethore jcef-natives-linux-amd64 "$release_tag"
./upload_artifact.sh io.github.trethore jcef-natives-linux-arm64 "$release_tag"

# Upload windows natives
./upload_artifact.sh io.github.trethore jcef-natives-windows-amd64 "$release_tag"

# Upload macosx natives
./upload_artifact.sh io.github.trethore jcef-natives-macosx-amd64 "$release_tag"
./upload_artifact.sh io.github.trethore jcef-natives-macosx-arm64 "$release_tag"

echo "Done uploading artifacts to GitHub Packages!"

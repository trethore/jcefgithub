#!/usr/bin/env bash
# shellcheck disable=SC2154
set -Eeuo pipefail

if [ ! $# -eq 2 ]; then
  echo "Usage: ./upload_artifacts.sh <build_meta_url> <mvn_version>"
  exit 1
fi

# CD to dir of this script
root=$(cd "$(dirname "$0")/../.." && pwd)
cd "$root"

# Set build info
. packaging/scripts/set_build_info.sh "$1" "$2"

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

uploader=packaging/scripts/upload_artifact.sh

"$uploader" io.github.trethore jcef-api "$release_tag"
"$uploader" io.github.trethore jcefgithub "$mvn_version"
for platform in linux-amd64 linux-arm64 windows-amd64 windows-arm64 macosx-amd64 macosx-arm64; do
  "$uploader" io.github.trethore "jcef-natives-$platform" "$release_tag"
done

echo "Done uploading artifacts to GitHub Packages!"

#!/usr/bin/env bash
# shellcheck disable=SC2154
set -Eeuo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)

mvn -q --no-transfer-progress install:install-file \
  -Dfile="/jcefout/jcef-api-$release_tag.jar" -DpomFile="/jcefout/jcef-api-$release_tag.pom"

args=(-f "$root/library/pom.xml" clean package "-Drevision=$mvn_version" "-Djcef.version=$release_tag")
for platform in linux-amd64 linux-arm64 windows-amd64 windows-arm64 macosx-amd64 macosx-arm64; do
  property=${platform//-/.}
  jar="/jcefout/jcef-natives-$platform-$release_tag.jar"
  [[ -f $jar ]] || { echo "Missing native artifact: $jar" >&2; exit 1; }
  args+=("-Dsha256.$property=$(sha256sum "$jar" | cut -d' ' -f1)")
done
mvn -q --no-transfer-progress "${args[@]}"

for suffix in .jar -all-relocated.jar -javadoc.jar -sources.jar; do
  cp "$root/library/target/jcefgithub-$mvn_version$suffix" /jcefout/
done
cp "$root/library/.flattened-pom.xml" "/jcefout/jcefgithub-$mvn_version.pom"

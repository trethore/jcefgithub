#!/bin/bash
set -e

if [ ! $# -eq 3 ]
  then
    echo "Usage: ./upload_artifact.sh <groupId> <artifactId> <version>"
    exit 1
fi

groupId=$1
artifactId=$2
version=$3

#CD to the upload dir
cd "$( dirname "$0" )" && cd upload

GITHUB_PACKAGES_URL="https://maven.pkg.github.com/${GITHUB_REPOSITORY}"

echo "Uploading $artifactId-$version to GitHub Packages..."

# Skip if artifact already exists (GitHub Packages rejects overwrites with 409).
groupPath="${groupId//./\/}"
baseUrl="$GITHUB_PACKAGES_URL/$groupPath/$artifactId/$version"

# Deploy the jar
jarFile="$artifactId-$version.jar"
fatJarFile="$artifactId-$version-all.jar"
pomFile="$artifactId-$version.pom"
sourcesFile="$artifactId-$version-sources.jar"
javadocFile="$artifactId-$version-javadoc.jar"

if [ -n "$GITHUB_ACTOR" ] && [ -n "$GITHUB_TOKEN" ]; then
  if curl -fsI -u "$GITHUB_ACTOR:$GITHUB_TOKEN" "$baseUrl/$pomFile" >/dev/null; then
    echo "Artifact $artifactId-$version already exists. Skipping."
    exit 0
  fi
fi

deployCmd="mvn -q --no-transfer-progress deploy:deploy-file \
  -DgroupId=$groupId \
  -DartifactId=$artifactId \
  -Dversion=$version \
  -Dpackaging=jar \
  -Dfile=$jarFile \
  -DrepositoryId=github \
  -Durl=$GITHUB_PACKAGES_URL \
  -DskipExisting=true"

# Add POM if it exists
if [ -f "$pomFile" ]; then
  deployCmd="$deployCmd -DpomFile=$pomFile"
else
  deployCmd="$deployCmd -DgeneratePom=true"
fi

# Add sources if they exist
if [ -f "$sourcesFile" ]; then
  deployCmd="$deployCmd -Dsources=$sourcesFile"
fi

# Add javadoc if it exists
if [ -f "$javadocFile" ]; then
  deployCmd="$deployCmd -Djavadoc=$javadocFile"
fi

# Add fat jar with classifier 'all' if it exists
if [ -f "$fatJarFile" ]; then
  deployCmd="$deployCmd -Dfiles=$fatJarFile -Dclassifiers=all -Dtypes=jar"
fi

eval $deployCmd

echo "Done uploading $artifactId-$version."

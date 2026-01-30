#!/bin/bash
set -e

if [ ! $# -eq 1 ]
  then
    echo "Usage: ./generate_jogl.sh <artifact>"
    echo ""
    echo "artifact: the artifact to create (e.g. jogl-all or gluegen-rt)"
    exit 1
fi

#CD to base dir of this repository
cd "$( dirname "$0" )" && cd ..

#Clear build dir
REPO_ROOT="$(pwd)"
rm -rf build
mkdir build
cd build

if [ -z "$jogl_build" ]; then
  jogl_build="2.5.0"
  echo "jogl_build not set, defaulting to $jogl_build"
fi
if [ -z "$jogl_download" ]; then
  jogl_download="https://jogamp.org/deployment/maven"
  echo "jogl_download not set, defaulting to $jogl_download"
fi

echo "Creating $1 with version $jogl_build..."
export platform=*
export release_download_url=$jogl_download

LIBS_DIR="$REPO_ROOT/libs"
mkdir -p "$LIBS_DIR"

if [[ "$1" == "jogl-all" ]] ; then
   group_path="org/jogamp/jogl"
elif [[ "$1" == "gluegen-rt" ]] ; then
   group_path="org/jogamp/gluegen"
else
   echo "Unsupported artifact: $1"
   exit 1
fi

version="$jogl_build"
base_url="$jogl_download/$group_path/$1/$version"
echo "Using base URL: $base_url"

native_classifiers=(linux-aarch64 linux-amd64 linux-armv6hf macosx-universal windows-amd64 windows-i586)
if [ -n "$JOGL_NATIVE_CLASSIFIERS" ]; then
  IFS=',' read -r -a native_classifiers <<< "$JOGL_NATIVE_CLASSIFIERS"
fi

download_if_missing() {
  local filename="$1"
  local dest="$LIBS_DIR/$filename"
  if [ ! -s "$dest" ]; then
    echo "Downloading $filename..."
    if ! curl -f -L -o "$dest" "$base_url/$filename"; then
      echo "Failed to download $base_url/$filename"
      rm -f "$dest"
      exit 1
    fi
  fi
}

#Fetch artifact
echo "Fetching artifacts..."
download_if_missing "$1-$version.jar"
for classifier in "${native_classifiers[@]}"; do
  download_if_missing "$1-$version-natives-$classifier.jar"
done
download_if_missing "$1-$version-sources.jar"
download_if_missing "$1-$version-javadoc.jar"

cp "$LIBS_DIR/$1-$version.jar" "$1.jar"
for classifier in "${native_classifiers[@]}"; do
  cp "$LIBS_DIR/$1-$version-natives-$classifier.jar" "$1-natives-$classifier.jar"
done

#Extract artifacts
echo "Extracting..."
set +e
unzip '*.jar'
rm *.jar
set -e

#Remove meta-inf as it contains wrong hashes
rm -r META-INF

#Compress contents
echo "Compressing package..."
zip -r "$1-$jogl_build.jar" *

#Generate a pom file
echo "Generating pom..."
./../scripts/fill_template.sh "../templates/$1/pom.xml" "$1-$jogl_build.pom"

#Use maven-provided sources/javadoc
cp "$LIBS_DIR/$1-$version-sources.jar" "$1-$jogl_build-sources.jar"
cp "$LIBS_DIR/$1-$version-javadoc.jar" "$1-$jogl_build-javadoc.jar"

#Move built artifacts to export dir
echo "Exporting artifacts..."
mv $1-$jogl_build.jar /jcefout
mv $1-$jogl_build-sources.jar /jcefout
mv $1-$jogl_build-javadoc.jar /jcefout
mv $1-$jogl_build.pom /jcefout

#Done
echo "Done generating $1 with version $jogl_build"

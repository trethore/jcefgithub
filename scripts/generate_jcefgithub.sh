#!/bin/bash
set -e

#CD to base dir of this repository
cd "$( dirname "$0" )" && cd ..

#Clear build dir
rm -rf build
mkdir build
cd build

echo "Creating jcefgithub with tag $mvn_version..."
export platform=\*
export release_download_url=$release_url

#Copy project
cp -r ../jcefgithub .

#Generate pom
rm -f jcefgithub/pom.xml
./../scripts/fill_template.sh jcefgithub/pom.xml.template jcefgithub/pom.xml

#Generate metadata resource
rm -f jcefgithub/src/main/resources/jcefgithub_build_meta.json
./../scripts/fill_template.sh jcefgithub/src/main/resources/jcefgithub_build_meta.json.template jcefgithub/src/main/resources/jcefgithub_build_meta.json

#Install required artifacts to local repo
mvn -q --no-transfer-progress install:install-file -Dfile=/jcefout/jogl-all-$jogl_build.jar -DpomFile=/jcefout/jogl-all-$jogl_build.pom
mvn -q --no-transfer-progress install:install-file -Dfile=/jcefout/gluegen-rt-$jogl_build.jar -DpomFile=/jcefout/gluegen-rt-$jogl_build.pom
mvn -q --no-transfer-progress install:install-file -Dfile=/jcefout/jcef-api-$release_tag.jar -DpomFile=/jcefout/jcef-api-$release_tag.pom

#Perform build
cd jcefgithub
mvn -q --no-transfer-progress clean package source:jar javadoc:jar
cd ..

##########################
#Move built artifacts to export dir
##########################
echo "Exporting artifacts..."
mv jcefgithub/target/jcefgithub-$mvn_version.jar /jcefout
mv jcefgithub/target/jcefgithub-$mvn_version-javadoc.jar /jcefout
mv jcefgithub/target/jcefgithub-$mvn_version-sources.jar /jcefout
mv jcefgithub/pom.xml /jcefout/jcefgithub-$mvn_version.pom

#Done
echo "Done generating jcefgithub for $mvn_version"

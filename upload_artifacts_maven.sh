#!/bin/bash
set -e

if [ ! $# -eq 2 ]; then
  echo "Usage: ./upload_artifacts_maven.sh <build_meta_url> <mvn_version>"
  exit 1
fi

required_env_vars=(
  MAVEN_CENTRAL_USERNAME
  MAVEN_CENTRAL_PASSWORD
  MAVEN_GPG_PRIVATE_KEY
  MAVEN_GPG_PASSPHRASE
)

required_commands=(
  base64
  curl
  gpg
  gpgconf
  jq
  md5sum
  sha1sum
  sha256sum
  sha512sum
  zip
)

for env_var in "${required_env_vars[@]}"; do
  if [ -z "${!env_var}" ]; then
    echo "Missing required environment variable: $env_var"
    exit 1
  fi
done

for cmd in "${required_commands[@]}"; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "Missing required command: $cmd"
    exit 1
  fi
done

# CD to dir of this script
cd "$( dirname "$0" )"

# Set build info
. scripts/set_build_info.sh "$1" "$2"

group_id="io.github.trethore"
group_path="${group_id//./\/}"
maven_central_base_url="https://repo1.maven.org/maven2"
staged_artifact_count=0

rm -rf upload
mkdir upload
cp out/* upload/

rm -rf central-bundle
mkdir -p central-bundle

mkdir -p "$HOME/.gnupg"
chmod 700 "$HOME/.gnupg"
if [ ! -f "$HOME/.gnupg/gpg.conf" ] || ! grep -q "pinentry-mode loopback" "$HOME/.gnupg/gpg.conf"; then
  echo "pinentry-mode loopback" >> "$HOME/.gnupg/gpg.conf"
fi
if [ ! -f "$HOME/.gnupg/gpg-agent.conf" ] || ! grep -q "allow-loopback-pinentry" "$HOME/.gnupg/gpg-agent.conf"; then
  echo "allow-loopback-pinentry" >> "$HOME/.gnupg/gpg-agent.conf"
fi
gpgconf --kill gpg-agent || true

printf '%s' "$MAVEN_GPG_PRIVATE_KEY" | gpg --batch --import

generate_checksum_files() {
  local file="$1"
  md5sum "$file" | cut -d' ' -f1 > "$file.md5"
  sha1sum "$file" | cut -d' ' -f1 > "$file.sha1"
  sha256sum "$file" | cut -d' ' -f1 > "$file.sha256"
  sha512sum "$file" | cut -d' ' -f1 > "$file.sha512"
}

sign_and_checksum() {
  local file="$1"
  gpg --batch --yes --armor --pinentry-mode loopback --passphrase "$MAVEN_GPG_PASSPHRASE" --detach-sign "$file"
  generate_checksum_files "$file"
  generate_checksum_files "$file.asc"
}

artifact_exists_on_maven_central() {
  local artifact_id="$1"
  local version="$2"
  local pom_url="$maven_central_base_url/$group_path/$artifact_id/$version/$artifact_id-$version.pom"
  curl -fsI "$pom_url" >/dev/null 2>&1
}

stage_artifact() {
  local artifact_id="$1"
  local version="$2"

  if artifact_exists_on_maven_central "$artifact_id" "$version"; then
    echo "Skipping $artifact_id:$version because it already exists on Maven Central"
    return
  fi

  local artifact_dir="central-bundle/$group_path/$artifact_id/$version"
  local basename="$artifact_id-$version"
  mkdir -p "$artifact_dir"

  local main_jar="upload/$basename.jar"
  local pom_file="upload/$basename.pom"
  local sources_jar="upload/$basename-sources.jar"
  local javadoc_jar="upload/$basename-javadoc.jar"
  local relocated_jar="upload/$basename-all-relocated.jar"

  if [ ! -f "$main_jar" ]; then
    echo "Missing artifact file: $main_jar"
    exit 1
  fi

  if [ ! -f "$pom_file" ]; then
    echo "Missing artifact file: $pom_file"
    exit 1
  fi

  if [ ! -f "$sources_jar" ]; then
    echo "Missing required sources jar: $sources_jar"
    exit 1
  fi

  if [ ! -f "$javadoc_jar" ]; then
    echo "Missing required javadoc jar: $javadoc_jar"
    exit 1
  fi

  local artifact_files=(
    "$main_jar"
    "$pom_file"
    "$sources_jar"
    "$javadoc_jar"
  )

  if [ -f "$relocated_jar" ]; then
    artifact_files+=("$relocated_jar")
  fi

  for source_file in "${artifact_files[@]}"; do
    local destination_file="$artifact_dir/$(basename "$source_file")"
    cp "$source_file" "$destination_file"
    sign_and_checksum "$destination_file"
  done

  staged_artifact_count=$((staged_artifact_count + 1))
  echo "Prepared $artifact_id:$version for Maven Central bundle"
}

echo "Preparing artifacts for Maven Central..."

stage_artifact "jcef-api" "$release_tag"
stage_artifact "jcefgithub" "$mvn_version"
stage_artifact "jcef-natives-linux-amd64" "$release_tag"
stage_artifact "jcef-natives-linux-arm64" "$release_tag"
stage_artifact "jcef-natives-windows-amd64" "$release_tag"
stage_artifact "jcef-natives-windows-arm64" "$release_tag"
stage_artifact "jcef-natives-macosx-amd64" "$release_tag"
stage_artifact "jcef-natives-macosx-arm64" "$release_tag"

if [ "$staged_artifact_count" -eq 0 ]; then
  echo "All requested artifacts already exist on Maven Central. Nothing to publish."
  exit 0
fi

rm -f central-bundle.zip
cd central-bundle
zip -qr ../central-bundle.zip .
cd ..

auth_token="$(printf '%s:%s' "$MAVEN_CENTRAL_USERNAME" "$MAVEN_CENTRAL_PASSWORD" | base64 | tr -d '\n')"
deployment_name="io.github.trethore:jcefgithub:$mvn_version"
encoded_deployment_name="$(printf '%s' "$deployment_name" | jq -sRr @uri)"
upload_url="https://central.sonatype.com/api/v1/publisher/upload?name=$encoded_deployment_name&publishingType=AUTOMATIC"

echo "Uploading bundle to Maven Central Publisher Portal..."
deployment_id="$(curl -fsS --request POST \
  --header "Authorization: Bearer $auth_token" \
  --form "bundle=@central-bundle.zip;type=application/octet-stream" \
  "$upload_url")"

if [ -z "$deployment_id" ]; then
  echo "Upload failed: empty deployment id"
  exit 1
fi

echo "Deployment id: $deployment_id"

status_url="https://central.sonatype.com/api/v1/publisher/status?id=$deployment_id"

echo "Waiting for deployment to reach PUBLISHING state..."
for _ in {1..180}; do
  status_response="$(curl -fsS --request POST --header "Authorization: Bearer $auth_token" "$status_url")"
  deployment_state="$(printf '%s' "$status_response" | jq -r '.deploymentState')"

  echo "Deployment state: $deployment_state"

  case "$deployment_state" in
    PUBLISHING)
      echo "Deployment entered PUBLISHING state."
      exit 0
      ;;
    FAILED)
      echo "Deployment failed."
      printf '%s' "$status_response" | jq
      exit 1
      ;;
  esac

  sleep 10
done

echo "Timed out waiting for deployment to reach PUBLISHING state."
exit 1

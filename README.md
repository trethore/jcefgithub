# JCEF GitHub

Maven artifacts and a small Java bootstrap library for the Java Chromium Embedded Framework (JCEF).

![Browser demo](docs/assets/demo.png)

## Requirements

- Java 17 or later
- Linux, Windows, or macOS on AMD64 or ARM64

## Installation

Use the latest version shown on the repository's [releases](../../releases) page or Maven Central:

```xml
<dependency>
  <groupId>io.github.trethore</groupId>
  <artifactId>jcefgithub</artifactId>
  <version>RELEASE_VERSION</version>
</dependency>
```

```groovy
implementation "io.github.trethore:jcefgithub:RELEASE_VERSION"
```

### Artifact selection

| Artifact | Purpose |
| --- | --- |
| `jcef-api` | The Java classes built from the [JCEF fork](https://github.com/trethore/jcef). It is pulled transitively by `jcefgithub`; applications normally do not declare it directly. |
| `jcefgithub` | The runtime bootstrap wrapper. It selects, downloads, verifies, extracts, and initializes the native JCEF bundle for the current platform. |
| `jcef-natives-<os>-<arch>` | One native JCEF/CEF bundle for a specific operating system and architecture. Add one when natives must be available from the classpath without a runtime download. |
| `jcefgithub:all-relocated` | Optional classifier containing the wrapper and its Gson and Commons Compress dependencies under relocated package names. Use it to reduce dependency conflicts; it does not contain JCEF natives. |

The standard `jcefgithub` dependency downloads and extracts the correct native bundle on first use. Downloads are verified against release-specific SHA-256 hashes. To avoid a runtime download, add **exactly one** native artifact matching the target platform:

- `jcef-natives-linux-amd64`
- `jcef-natives-linux-arm64`
- `jcef-natives-windows-amd64`
- `jcef-natives-windows-arm64`
- `jcef-natives-macosx-amd64`
- `jcef-natives-macosx-arm64`

Native artifacts use the JCEF/CEF build version shown in the corresponding release notes; this is not necessarily the same as `RELEASE_VERSION`. For example:

```xml
<dependency>
  <groupId>io.github.trethore</groupId>
  <artifactId>jcef-natives-linux-amd64</artifactId>
  <version>JCEF_NATIVE_VERSION_FROM_RELEASE_NOTES</version>
</dependency>
```

```groovy
implementation "io.github.trethore:jcef-natives-linux-amd64:JCEF_NATIVE_VERSION_FROM_RELEASE_NOTES"
```

To use the relocated wrapper, request the classifier instead of adding both wrapper variants:

```xml
<dependency>
  <groupId>io.github.trethore</groupId>
  <artifactId>jcefgithub</artifactId>
  <version>RELEASE_VERSION</version>
  <classifier>all-relocated</classifier>
</dependency>
```

```groovy
implementation "io.github.trethore:jcefgithub:RELEASE_VERSION:all-relocated"
```

### Package repositories

Releases may be published to both Maven Central and GitHub Packages. The artifacts are built from the same release inputs, but the repositories have different access requirements:

- **Maven Central** is the recommended source for consumers. It works with the standard Maven Central repository and requires no GitHub credentials.
- **GitHub Packages** is the repository-scoped publication target. Consumers must configure `https://maven.pkg.github.com/trethore/jcefgithub` and authenticate with a GitHub token that can read packages.

The manually dispatched [Publish workflow](.github/workflows/publish.yml) can target Maven Central, GitHub Packages, or both.

## Usage

```java
CefAppBuilder builder = new CefAppBuilder();
builder.setInstallDir(new File("jcef-bundle"));
builder.setProgressHandler(new ConsoleProgressHandler());
builder.addJcefArgs("--disable-gpu");
builder.getCefSettings().windowless_rendering_enabled = true;
builder.setAppHandler(new MavenCefAppHandlerAdapter() {});

CefApp app = builder.build();
```

Do not call `CefApp.addAppHandler(...)` directly; use `CefAppBuilder.setAppHandler(...)` for macOS compatibility.

Custom native mirrors can be configured through `setMirrors(Collection<String>)`. Supported placeholders are `{mvn_version}`, `{platform}`, and `{tag}`. Downloaded native jars are verified using release-specific SHA-256 metadata before extraction.

On macOS, JDK 17+ may require:

```text
--add-opens java.desktop/sun.awt=ALL-UNNAMED
--add-opens java.desktop/sun.lwawt=ALL-UNNAMED
--add-opens java.desktop/sun.lwawt.macosx=ALL-UNNAMED
```

## Development and hosting

- [Building](docs/building.md)
- [Architecture](docs/architecture.md)
- [Publishing/self-hosting](docs/publishing.md)
- [Contributing](CONTRIBUTING.md)
- [Security policy](SECURITY.md)

### How releases connect

The [jcefbuild](https://github.com/trethore/jcefbuild) pipeline builds and tests JCEF/CEF for every supported platform and publishes a `build_meta.json`. That metadata records the JCEF commit, CEF version, platform download URLs, and release tag. This repository's [release pipeline](.github/workflows/publish.yml) accepts the HTTPS URL of that file, validates it, then:

1. creates `jcef-api` from the matching JCEF sources;
2. creates each platform-native artifact from the recorded build download;
3. embeds the native artifact hashes in the `jcefgithub` runtime wrapper; and
4. publishes the resulting Maven artifacts and release metadata.

See the [local build architecture](docs/architecture.md), [publishing process](docs/publishing.md), [JCEF documentation](https://github.com/trethore/jcef/tree/master/docs), and [jcefbuild pipeline](https://github.com/trethore/jcefbuild/actions/workflows/build.yml).

### Support and issue routing

- [jcefgithub](../../issues) -> Maven/Gradle coordinates, publishing, artifact metadata, native downloading/extraction, bootstrap initialization, and this documentation.
- [jcefbuild](https://github.com/trethore/jcefbuild/issues) -> platform build failures, missing build artifacts, CI images, and `build_meta.json` produced by the native build pipeline.
- [trethore/jcef](https://github.com/trethore/jcef/issues) -> Java/native JCEF fork source, fork-specific behavior, and Java 17+ compatibility.
- [upstream JCEF](https://github.com/chromiumembedded/java-cef/issues) -> issues reproducible with upstream JCEF and unrelated to this fork, packaging, or build infrastructure.
- [upstream CEF](https://github.com/chromiumembedded/cef/issues) -> Chromium Embedded Framework behavior below the JCEF Java/native binding.

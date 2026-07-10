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

The normal artifact downloads and extracts the correct native bundle on first use. To avoid runtime downloads, add exactly one `jcef-natives-<platform>` artifact matching the JCEF version listed in the release notes.

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

Report artifact/bootstrap defects in this repository. Report upstream JCEF or CEF defects to their corresponding upstream projects.

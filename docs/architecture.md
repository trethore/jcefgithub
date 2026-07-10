# Architecture

- `library/` -> Java API, native installation, verification, and JCEF initialization.
- `packaging/` -> reproducible Docker build, artifact templates, and publishing scripts.
- `.github/workflows/ci.yml` -> pull-request verification.
- `.github/workflows/publish.yml` -> release orchestration.

At runtime, a platform-specific native jar is loaded from the classpath or downloaded from a configured mirror. Downloaded jars are checked against SHA-256 values embedded during the release build before extraction.

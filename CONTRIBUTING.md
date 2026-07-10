# Contributing

## Requirements

- JDK 17
- Docker with Compose for full artifact generation

Run `./mvnw verify` before submitting a change. Shell scripts must pass ShellCheck.

Library code lives in `library/`. Release packaging lives in `packaging/`. Pull requests must not include generated artifacts or release-version-only README changes.

See `docs/building.md` for local build instructions and `docs/publishing.md` for the release process.

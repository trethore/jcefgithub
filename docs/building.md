# Building

## Library

Requirements: JDK 17 and `curl`.

```sh
./mvnw verify
```

The default JCEF API coordinate is declared in the root and library POMs. A release build overrides `revision` and `jcef.version` from validated jcefbuild metadata.

## Complete artifact set

```sh
./generate_artifacts.sh https://example.invalid/build_meta.json 146.0.10.1
```

This uses `packaging/compose.yml` and writes generated artifacts to `out/`.

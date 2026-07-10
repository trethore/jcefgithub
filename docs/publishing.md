# Publishing

Publishing is performed through the manually dispatched `Publish` workflow.

1. Supply an HTTPS `build_meta.json` URL from a completed jcefbuild release.
2. Supply a unique Maven version.
3. Select GitHub, Maven Central, or both.

The workflow validates metadata, builds once, verifies native hashes, and gives each publication job only its required permissions. Concurrency prevents two runs from publishing the same Maven version simultaneously.

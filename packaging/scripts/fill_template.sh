#!/usr/bin/env bash
set -Eeuo pipefail
[[ $# -eq 2 ]] || { echo "Usage: $0 <template> <destination>" >&2; exit 2; }
template=$1 destination=$2
python3 - "$template" "$destination" <<'PY'
import os, pathlib, sys
source = pathlib.Path(sys.argv[1]).read_text(encoding="utf-8")
for key in ("platform", "release_tag", "release_url", "jcef_url", "release_download_url", "mvn_version"):
    source = source.replace("{" + key + "}", os.environ.get(key, ""))
pathlib.Path(sys.argv[2]).write_text(source, encoding="utf-8")
PY

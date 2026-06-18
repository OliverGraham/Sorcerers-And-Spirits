#!/usr/bin/env python3
"""Regenerate remoteconfig.template.json from the per-parameter source files.

The Firebase Remote Config CLI template stores each parameter value as a *stringified* JSON blob,
which is painful and error-prone to hand-edit. Instead, edit the human-friendly per-parameter source
files below, then run this script to rebuild the combined template before `firebase deploy`.

    python3 remote-config/build-template.py
"""

import json
import pathlib

REPO_ROOT = pathlib.Path(__file__).resolve().parent.parent

# Remote Config key -> human-editable source file (repo-relative). Each is a bundled app resource so
# the same version-controlled file is the in-app default and the value deployed to Firebase.
SOURCES = {
    "character_class_stats": "composeApp/src/commonMain/composeResources/files/remote_config/character_class_stats.json",
    "random_traveler_names": "composeApp/src/commonMain/composeResources/files/remote_config/random_traveler_names.json",
}

OUTPUT = REPO_ROOT / "remote-config" / "remoteconfig.template.json"


def main() -> None:
    parameters = {}
    for key, source in SOURCES.items():
        value = json.loads((REPO_ROOT / source).read_text())
        parameters[key] = {
            "defaultValue": {"value": json.dumps(value, separators=(",", ":"))},
            "valueType": "JSON",
        }

    OUTPUT.write_text(json.dumps({"parameters": parameters}, indent=2) + "\n")
    print(f"Wrote {OUTPUT.relative_to(REPO_ROOT)} from {len(SOURCES)} source file(s).")


if __name__ == "__main__":
    main()

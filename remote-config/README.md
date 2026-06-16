# Remote Config

Version-controlled copies of this app's Firebase Remote Config parameter values. These files are the
source of truth in git; the Firebase Console is where they are actually served from. Keep the two in
sync — editing a file here does **not** change production until it is published to Firebase.

## Parameters

| File | Remote Config key | Consumed by |
|------|-------------------|-------------|
| `character_class_stats.json` | `character_class_stats` | `RemoteConfigCharacterClassProvider` via `RemoteConfigKeys.CHARACTER_CLASS_STATS` |

### `character_class_stats.json`

A JSON object keyed by `CharacterClassType` enum name (case-sensitive — `"Rogue"`, not `"rogue"`).
Each class must provide all four numeric stats; a misspelled/missing class is skipped by the app and
falls back to its in-app default. Extra fields are ignored, so the schema can grow safely.

The in-app defaults (`defaultCharacterClassStats()`) mirror these values, so the game behaves
identically before the parameter is published or when a device is offline on first launch.

## Publishing to Firebase

**Console (manual):** Run → Remote Config → the `character_class_stats` parameter → paste the file
contents into the JSON default value → Save → **Publish changes**.

**Firebase CLI (optional, config-as-code):** the values above can instead live in a full Remote
Config template and be deployed with `firebase deploy --only remoteconfig`. The template wraps each
value as a stringified parameter, e.g.:

```json
{
  "parameters": {
    "character_class_stats": {
      "defaultValue": { "value": "<contents of character_class_stats.json as a string>" }
    }
  }
}
```

Use `firebase remoteconfig:get -o remoteconfig.template.json` to pull the current live template as a
starting point.

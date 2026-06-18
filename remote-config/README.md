# Remote Config

Version-controlled copies of this app's Firebase Remote Config parameter values. These files are the
source of truth in git; the Firebase Console is where they are actually served from. Keep the two in
sync — editing a file here does **not** change production until it is published to Firebase.

**`remoteconfig.template.json` is the single combined artifact you deploy** (all parameters in one
file). You don't hand-edit it — edit the human-friendly per-parameter source files below, then
regenerate it (see [Publishing to Firebase](#publishing-to-firebase)).

## Parameters

Each parameter has a human-editable source file (what you change) and is rolled up into the combined
template for deploy.

All source files are **bundled into the app** as Compose resources under
`composeApp/src/commonMain/composeResources/files/remote_config/`, so each version-controlled file
is
both the in-app default (read at startup, used for `setDefaults` and the offline/first-launch
fallback) and the value published to Firebase.

| Source file (edit this)      | Remote Config key       | Consumed by                                                                       |
|------------------------------|-------------------------|-----------------------------------------------------------------------------------|
| `character_class_stats.json` | `character_class_stats` | `RemoteConfigCharacterClassProvider` via `RemoteConfigKeys.CHARACTER_CLASS_STATS` |
| `random_traveler_names.json` | `random_traveler_names` | `RemoteConfigRandomNameProvider` via `RemoteConfigKeys.RANDOM_TRAVELER_NAMES`     |

### `character_class_stats.json`

A JSON object keyed by `CharacterClassType` enum name (case-sensitive — `"Rogue"`, not `"rogue"`).
Each class must provide all four numeric stats; a misspelled/missing class is skipped by the app and
falls back to its per-class factory default. Extra fields are ignored, so the schema can grow
safely.

This bundled file is the in-app default (`FirebaseRemoteConfigDataSource` reads it at startup for
`setDefaults` and the offline fallback). The `CharacterClass` factory stat literals remain only as
the provider's last-resort per-class fallback; the `BundledRemoteConfigDefaultsTest` drift guard
asserts the two are identical, so they can never silently diverge (SS-059).

### `random_traveler_names.json`

A JSON array of placeholder display names shown before sign-in. The same version-controlled file is
both the in-app default (read at startup, used for `setDefaults` and the offline/first-launch
fallback) and the value published to Firebase. There is no hardcoded Kotlin
copy to keep in sync — only a single ultimate fallback name guards the pre-initialize window. This
is
the bundled-default pattern that SS-085 generalizes to every parameter.

## Publishing to Firebase

### Combined template (preferred — all parameters at once)

`remoteconfig.template.json` holds every parameter in one Firebase Remote Config template. Each
value
is stored as a *stringified* JSON blob (escaped quotes), which is why you never edit it by hand.

1. Edit the per-parameter source file(s) in the table above.
2. Regenerate the template:
   ```sh
   python3 remote-config/build-template.py
   ```
3. Deploy:
   ```sh
   firebase deploy --only remoteconfig
   ```

To pull the current live template as a starting point:
`firebase remoteconfig:get -o remote-config/remoteconfig.template.json`.

### Console (manual, single parameter)

Run → Remote Config → the parameter (e.g. `character_class_stats`) → paste the matching source
file's contents into the JSON default value → Save → **Publish changes**. Use this for a one-off
tweak without the CLI; the combined template above is the source of truth otherwise.

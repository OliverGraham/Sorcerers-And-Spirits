# Apple account-deletion token revocation (SS-296)

Apps Script backend that revokes a user's Sign in with Apple tokens when they delete their account,
as required by App Store Guideline 5.1.1(v). The iOS app re-authenticates with Apple at delete time,
gets a fresh one-time `authorizationCode`, and POSTs it here; this script exchanges it for a refresh
token and revokes it. No tokens are stored.

## Files

- `Code.gs` — the endpoint (`doPost`).
- `appsscript.json` — manifest (enables `UrlFetchApp` + anonymous web-app access).
- `jsrsasign` — **not checked in.** You paste it in during deploy (step 2); it provides ES256 JWT
  signing, which Apps Script lacks natively.

## Deploy steps

1. **Create the project.** Go to <https://script.google.com> → New project. Paste `Code.gs` into the
   default `Code.gs` file. Open Project Settings → enable "Show appsscript.json" and paste
   `appsscript.json` over the generated manifest.

2. **Add jsrsasign.** Add a new script file named `jsrsasign.gs`. At the very top (line 1), paste
   this shim, then paste the full library below it:

   ```js
   var navigator = { userAgent: '' };
   var window = this;
   ```

   Library contents: <https://raw.githubusercontent.com/kjur/jsrsasign/master/jsrsasign-all-min.js>.
   The shim is required because jsrsasign does browser feature-detection (`navigator.userAgent`) at
   load time, which the Apps Script server runtime lacks — without it the whole script fails to load
   with `ReferenceError: navigator is not defined`.

3. **Set Script Properties** (Project Settings → Script Properties → Add). All five are required:

   | Property            | Value                                                                 |
   |---------------------|-----------------------------------------------------------------------|
   | `APPLE_PRIVATE_KEY` | Full contents of your `.p8` key, incl. the `-----BEGIN PRIVATE KEY-----` / `-----END PRIVATE KEY-----` lines |
   | `APPLE_KEY_ID`      | The 10-char Key ID of that `.p8` key                                  |
   | `APPLE_TEAM_ID`     | Your Apple Developer Team ID                                          |
   | `APPLE_BUNDLE_ID`   | The iOS app's bundle identifier (native sign-in uses the bundle ID as the client_id, **not** the web Services ID) |
   | `SHARED_TOKEN`      | Any long random string; the app sends it to authenticate to this endpoint |

4. **Deploy.** Deploy → New deployment → type "Web app", execute as yourself, access "Anyone". Copy
   the `/exec` URL. Authorize the `script.external_request` scope when prompted.

5. **Publish config.** In the public `sorcerers-and-spirits-config` repo, add `apple_revoke.json`:

   ```json
   {
     "endpointUrl": "<the /exec URL from step 4>",
     "token": "<the SHARED_TOKEN from step 3>"
   }
   ```

   The bundled copy at `composeApp/src/commonMain/composeResources/files/remote_config/apple_revoke.json`
   stays empty — a blank `endpointUrl` makes the client skip revocation gracefully, so the app is safe
   before the config is live.

## Test it

```
curl -L -X POST "<exec URL>" \
  -H "Content-Type: application/json" \
  -d '{"token":"<SHARED_TOKEN>","authorizationCode":"test"}'
```

A bad/expired code returns `{"success":false}` (the exchange fails) — that confirms the auth check and
signing path work. A real code from a live sign-in returns `{"success":true}`.

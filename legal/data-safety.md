# Store data-safety / privacy-label reference (SS-292)

Internal reference for filling the Google Play **Data safety** form and the App Store **App Privacy**
nutrition label. Mirrors what the app actually collects as of 2026-10-07. Keep this in sync with
`privacy-policy.html` (published) whenever the collected data changes.

Published policy URL: https://olivergraham.github.io/sorcerers-and-spirits-config/privacy-policy.html

## What the app collects (source of truth)

| Data | Where stored | Linked to user? | Why | Shared with others? |
|------|--------------|-----------------|-----|---------------------|
| Account identifier (Firebase uid) | Firebase Auth | Yes | Account / app functionality | No |
| Email (only if email/password sign-in used) | Firebase Auth | Yes | Account management | No |
| Display name (user-chosen) | Firestore + Realtime DB | Yes | App functionality, multiplayer | Yes — other players + public leaderboard |
| Game statistics (match/gauntlet counts, KOs, attacks, abilities, times-selected) | Firestore | Yes | App functionality, leaderboards | Yes — rating + win totals on public leaderboard |
| Achievements unlocked (+ timestamps) | Firestore | Yes | App functionality | No |
| In-match state / presence | Realtime DB | Yes | Multiplayer gameplay | Yes — players in the same session |
| Session cache (uid + display name) | On device only | Yes | Keep user signed in | No |
| Crash logs + diagnostics (stack trace, device model, OS version, app state) | Firebase Crashlytics | Yes (tagged with uid) | Diagnose/fix crashes | No — Google as processor only. **Android only** (iOS deferred). |

**Not collected:** location, contacts, photos/media, advertising ID, usage analytics. No ads.

## Google Play — Data safety answers

- **Does your app collect or share any of the required user data types?** Yes.
- **Data types collected:**
  - *Personal info → Name* — the display name. Collected, linked to the user, shared (visible to
    other players / leaderboard). Not optional (required to play).
  - *Personal info → Email address* — only when the user registers with email/password. Collected,
    linked, not shared. Optional (only that sign-in path).
  - *App activity → Other user-generated content / in-app actions* — display name + game stats +
    achievements + in-match state. Collected, linked, shared (stats/leaderboard portion).
  - *App info and performance → Crash logs* and *→ Diagnostics* — via Firebase Crashlytics.
    Collected, linked to the user (tagged with uid), not shared. (Android only for now.)
- **Is all collected data encrypted in transit?** Yes (Firebase TLS).
- **Do you provide a way to request data deletion?** Yes — in-app account deletion, plus email
  request. (Play also wants a web deletion URL — tracked separately in **SS-297**.)
- **Is data processed ephemerally?** No.
- **Account creation required?** Yes.

## Apple App Store — App Privacy answers

- **Data used to track you:** None.
- **Data linked to you:**
  - *Contact Info → Name* (display name)
  - *Contact Info → Email Address* (only for email/password sign-in)
  - *User Content → Other User Content* (game stats, achievements, in-match state)
  - *Identifiers → User ID* (Firebase account identifier)
  - *Diagnostics → Crash Data* and *→ Other Diagnostic Data* (Firebase Crashlytics; Android only
    today, but declare it for the shared app privacy label)
  - Purpose for all: **App Functionality**. Not used for tracking or advertising.
- **Data not linked to you:** None.
- No web-deletion-link requirement on iOS; in-app deletion suffices (SS-295).

## Placeholders to confirm before submitting

- **Developer / legal name** — currently "Link Labs" (from package id `com.linklabs.sorcerersandspirits`).
- **Contact email** — currently `og.singletonne@gmail.com`; swap if you want a dedicated address.

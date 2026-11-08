# Releasing

How a version gets from `main` to testers and to Google Play. Everything the workflow needs from you is
in the first two sections; the rest is the order of steps.

## 1. The upload key (once)

The app is signed with an **upload key** that only you hold. Create it on your machine, outside the
repository:

```sh
keytool -genkeypair -v -keystore upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

Keep `upload.jks` and both passwords somewhere safe (a password manager and one offline copy). Enrol the
app in **Play App Signing** when you create it in Play Console: Google then holds the real signing key,
and a lost upload key can be reset instead of ending the app.

For a local release build, create `keystore.properties` next to `build.gradle.kts` (git-ignored):

```properties
storeFile=/absolute/path/to/upload.jks
storePassword=...
keyAlias=upload
keyPassword=...
```

`./gradlew bundleRelease` then writes a signed bundle to `app/build/outputs/bundle/release/`. Without
that file the release build is unsigned, which is what CI checks on every pull request.

## 2. GitHub secrets (once)

Repository settings, Secrets and variables, Actions:

| Secret | Value | Required |
| --- | --- | --- |
| `SIGNING_KEYSTORE_BASE64` | `base64 -w0 upload.jks` | yes |
| `SIGNING_STORE_PASSWORD` | the keystore password | yes |
| `SIGNING_KEY_ALIAS` | `upload` | yes |
| `SIGNING_KEY_PASSWORD` | the key password | yes |
| `MAPS_API_KEY` | the Maps SDK for Android key | for a map in the release |
| `GOOGLE_SERVICES_JSON_BASE64` | `base64 -w0 app/google-services.json` | for Crashlytics, Analytics, Remote Config |

Without the last two the release still builds: Nearby shows a list instead of a map, and there is no
Firebase. Restrict the Maps key to the package `io.github.diiafyra.motoman` and the SHA-1 of the **Play
App Signing** certificate (Play Console, App integrity) as well as your upload key.

## 3. Cutting a version

1. In a pull request, set `versionName` and `versionCode` in `app/build.gradle.kts` and turn the
   Unreleased part of `CHANGELOG.md` into the version. The code follows
   `(major * 10000 + minor * 100 + patch) * 10 + b`, with `b` the release candidate number (1 to 8) or 9
   for a final release: `v1.0.0-rc.1` is 100001, `v1.0.0` is 100009. Play needs the code to rise with
   every upload.
2. Merge it, then tag `main` and push the tag:

   ```sh
   git switch main && git pull
   git tag -a v1.0.0-rc.1 -m "v1.0.0-rc.1" && git push origin v1.0.0-rc.1
   ```

3. The **Release** workflow checks the tag against the files (`scripts/check-version.sh`), builds,
   verifies the signature, keeps the bundle and the R8 mapping as a build artifact, and publishes a
   GitHub release (a pre-release for `-rc` tags) with the APK attached.
4. Upload the `.aab` from the workflow artifact to Play Console.

## 4. Play Console

- **Name.** Search Google Play for "Sổ Xe" before creating the app; the name cannot be reclaimed once
  taken, and the package name `io.github.diiafyra.motoman` can never change.
- **Closed testing.** A new personal developer account must run a closed test with a minimum number of
  testers for a minimum number of days before it can ask for production access. The numbers have changed
  more than once, so read the current rule in Play Console before planning dates. Recruit testers early.
- **Privacy policy.** Publish [privacy.md](privacy.md) at a public URL. The simplest is GitHub Pages
  (Settings, Pages, deploy from `main`, folder `/docs`); read the text once before you submit it, since
  you are the one who answers for it.
- **Data safety form.** Answer from the policy. Collected: crash logs and diagnostics, app interactions
  (four events), approximate and precise location (processed to find nearby places, not stored). Shared
  with: Google (Firebase) as a processor; the public Overpass service receives coordinates. No data is
  sold. Data is encrypted in transit. Users can delete data by deleting vehicles or uninstalling.
- **Permissions.** Location (Nearby) and notifications (reminders), both requested in context.
- **Content rating, target audience, ads.** No ads, no purchases, not directed at children.

## 5. Before promoting to production

- [ ] No crash in Crashlytics during the whole closed test
- [ ] Every v1.0 feature works in airplane mode, except Nearby
- [ ] One pass through the app with TalkBack on a real device
- [ ] The map renders (needs the Maps key) and "Directions" opens a maps app
- [ ] README has screenshots, the architecture diagram and the build steps; at least four ADRs

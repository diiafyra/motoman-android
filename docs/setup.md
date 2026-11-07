# Local setup

## Requirements

- Android Studio (latest stable), which bundles JDK 21
- Android SDK platform 37

## First build

1. Clone the repository and open it in Android Studio. Studio writes `local.properties` with your
   SDK path; from the command line, create it yourself:

   ```properties
   sdk.dir=/path/to/Android/Sdk
   ```

2. Run `./gradlew assembleDebug`.

## Secrets

| Name | Used for | Where |
| --- | --- | --- |
| `MAPS_API_KEY` | The map on the Nearby screen | `local.properties` (`MAPS_API_KEY=...`), or the environment variable in CI |

Create the key in Google Cloud (Maps SDK for Android) and restrict it to the package
`io.github.diiafyra.motoman` and your debug and release SHA-1. Without it the app builds and runs; the
Nearby screen lists places without a map.

Other secrets are added here as they are needed. When they are, they go in `local.properties` or in files listed in
`.gitignore`, never in the repository, and CI reads them from GitHub Secrets. This page lists each one
as it is added.

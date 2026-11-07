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

None are needed yet. When they are, they go in `local.properties` or in files listed in
`.gitignore`, never in the repository, and CI reads them from GitHub Secrets. This page lists each one
as it is added.

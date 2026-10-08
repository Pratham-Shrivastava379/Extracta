# Extracta release guide

Extracta's release build is minified, resource-shrunk, and ready to produce an
Android App Bundle for Google Play or a signed APK for direct distribution.
Signing secrets are never stored in the repository.

The primary distribution path is a signed Android App Bundle uploaded first to
Google Play internal testing, then promoted after device and policy review.
Direct APK distribution is a fallback for controlled portfolio testing only.

## Create and protect the upload key

Create the key outside this repository and back it up in a password manager or
other encrypted storage:

```powershell
keytool -genkeypair -v -keystore C:\secure\extracta-upload.jks -alias extracta-upload -keyalg RSA -keysize 4096 -validity 10000
```

Never commit the keystore or its passwords. The repository ignores `*.jks` and
`*.keystore` as an additional safeguard.

## Configure signing

Set all four values as environment variables, or put the same names in the
user-level Gradle properties file outside the repository:

```properties
EXTRACTA_RELEASE_STORE_FILE=C:\\secure\\extracta-upload.jks
EXTRACTA_RELEASE_STORE_PASSWORD=replace-me
EXTRACTA_RELEASE_KEY_ALIAS=extracta-upload
EXTRACTA_RELEASE_KEY_PASSWORD=replace-me
```

The build fails early if only some values are supplied. When none are supplied,
release artifacts remain unsigned so pull-request CI can still verify R8 and
resource shrinking.

## Build release artifacts

```powershell
.\gradlew.bat :app:clean :app:testDebugUnitTest :app:lintDebug :app:assembleRelease :app:bundleRelease --no-daemon
```

The signed artifacts are written to:

- `app/build/outputs/apk/release/app-release.apk`
- `app/build/outputs/bundle/release/app-release.aab`

Use the AAB for Google Play. Use the APK only for controlled direct
distribution. Before publishing, confirm the final application ID, version
code/name, store listing, privacy declaration, and Play App Signing settings.

## Final human gates

- Complete a TalkBack pass on Home, Capture, Details, entity actions, dialogs,
  and progress/error states.
- Review the privacy-safe screenshots in `docs/screenshots/`.
- Install the signed artifact on a clean device or emulator and verify its
  signing certificate before uploading it.

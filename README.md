# Gallery

A fast, pure-black Android gallery app. Kotlin + Jetpack Compose + Material 3, min SDK 26.

## Signing

The release workflow signs with the `GALLERY_KEYSTORE_BASE64`, `GALLERY_KEYSTORE_PASSWORD`,
`GALLERY_KEY_ALIAS`, and `GALLERY_KEY_PASSWORD` repository secrets when they exist.
On the very first run, if those secrets are not set yet, the workflow generates a release
key, signs with it, and uploads it as a CI artifact named `gallery-signing-keystore`
(not a release asset). Download that artifact, save it as the four secrets above, then
delete the artifact, so every later APK keeps the same signature and installs as an update.

## Releases

Version names are strictly numeric and increasing (0.0.1, 0.0.2, … 0.0.10, 0.1.0).
Each version is one push and one workflow run, publishing a signed APK to GitHub Releases.

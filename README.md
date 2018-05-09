P Locale Changing Example App
=============================

This app was created to showcase a useful blacklisted API in Android P that used to work on O to
change the system locale. Without reflective access to the methods required by
MainActivityKt.changeLocalePostO, there is no way for an application to change the system language.

To test this sample app, run:

```bash
./gradlew :app:installDebug
adb shell pm grant com.example.androidlocale android.permission.CHANGE_CONFIGURATION
```

Then launch the app, and press the "Change Language" button. Pull down the system drawer to look
at the system language after each press: it should change appropriately.

AOSP Issue: https://issuetracker.google.com/issues/79477176

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

AOSP Issue Text
---------------

NOTE: This component is private so you will typically only be able to see the issues you submit and are cc'd on.

Current java private API in use:

    val systemActivityManager = ActivityManager::class.java
        .getMethod("getService")
        .invoke(null)

    val systemConfig = systemActivityManager.javaClass
        .getMethod("getConfiguration")
        .apply { isAccessible = true }
        .invoke(systemActivityManager)
            as Configuration

    systemConfig.javaClass
        .getField("userSetLocale")
        .setBoolean(systemConfig, true)

    systemConfig.setLocale(locale)

    systemActivityManager.javaClass
        .getMethod("updateConfiguration", Configuration::class.java)
        .apply { isAccessible = true }
        .invoke(systemActivityManager, systemConfig)

Here is a full link to a simple project that showcases the issue:

https://github.com/mikeholler/Android-P-System-Locale-Issue

When running the above on P you get the following log:

    05-09 15:06:34.029 4107-4107/com.example.androidlocale W/e.androidlocal: Accessing hidden field Landroid/content/res/Configuration;->userSetLocale:Z (blacklist, reflection)

Why current public APIs are not sufficient for use case:

    val config = context.applicationContext.resources.configuration
    config.setLocale(locale)
    context.applicationContext.resources.updateConfiguration(config, null)

This public, now deprecated API `Resources.updateConfiguration` only changes the application's configuration, not the system's. The alternative offered for the deprecated method (Context.createConfigurationContext) is even worse, because it doesn't even change the application's configuration, just returns a new context. This context can be used to set the application's locale via Application.attachBaseContext, but that is only ever called once, and still only changes the app's locale (not the system's).

Our use case for changing the system locale is a B2B application for phones that are kept in a locked "kiosk" mode that only allows access to a subset of applications of our choosing. We want users to be able to select their language of choice, but often times the devices we use don't support the full range of language we would like. Therefore, we offer an expanded language selection utility that lists language we define as available, and use the above code on O to change the language.

We would like to see these APIs removed from the P blacklist so our application will continue to work.

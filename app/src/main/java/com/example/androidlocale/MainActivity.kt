package com.example.androidlocale

import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.annotation.RequiresApi
import kotlinx.android.synthetic.main.activity_main.*
import java.util.Locale

val supportedLocales = listOf("en-US", "es", "fr").map(Locale::forLanguageTag)

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        change_language.setOnClickListener {
            val current = currentLocale()
            val nextLocale = supportedLocales
                .indexOfFirst { it.toLanguageTag() == current.toLanguageTag() }
                .let { currentIndex -> (currentIndex + 1) % supportedLocales.size }
                .let { nextIndex -> supportedLocales[nextIndex]}

            changeLocaleSettings(nextLocale)
            finish()
            startActivity(intent)
        }
    }

}

fun Context.currentLocale(): Locale {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        resources.configuration.locales[0]
    } else {
        resources.configuration.locale
    }

}

/**
 * Under the current plugin architecture we need to use reflection to change the system locale
 * programmatically. In order for this to work the app has to be granted the
 * android.permission.CHANGE_CONFIGURATION permission explicitly via adb.
 *
 * https://github.com/jordansilva/Android-ChangeLocaleExample/blob/master/app/src/main/java/com/jordan/location/app/MainActivity.java
 *
 * @param locale
 */
fun changeLocaleSettings(locale: Locale): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        changeLocalePostO(locale)
    } else {
        changeLocalePreO(locale)
    }
}

@RequiresApi(api = Build.VERSION_CODES.O)
fun changeLocalePostO(locale: Locale): Boolean {
    try {
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
    } catch (e: Exception) {
        return false
    }

    return true
}

fun changeLocalePreO(locale: Locale): Boolean {
    try {
        //Getting by reflection the ActivityManagerNative
        val amnClass = Class.forName("android.app.ActivityManagerNative")

        // amn = ActivityManagerNative.getDefault();
        val methodGetDefault = amnClass.getMethod("getDefault")
        methodGetDefault.isAccessible = true
        val amn = methodGetDefault.invoke(amnClass)

        // config = amn.getConfiguration();
        val methodGetConfiguration = amnClass.getMethod("getConfiguration")
        methodGetConfiguration.isAccessible = true
        val config = methodGetConfiguration.invoke(amn) as Configuration

        // config.userSetLocale = true;
        val configClass = config.javaClass
        val f = configClass.getField("userSetLocale")
        f.setBoolean(config, true)

        // Update locale
        config.locale = locale

        // amn.updateConfiguration(config);
        val methodUpdateConfiguration = amnClass.getMethod(
            "updateConfiguration", Configuration::class.java
        )
        methodUpdateConfiguration.isAccessible = true
        methodUpdateConfiguration.invoke(amn, config)

    } catch (e: Exception) {
        return false
    }

    return true
}

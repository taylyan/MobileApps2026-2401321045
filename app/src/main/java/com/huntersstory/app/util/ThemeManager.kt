package com.huntersstory.app.util

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val PREFS_NAME = "theme_preferences"
    private const val KEY_NIGHT_MODE = "night_mode"

    fun restore(context: Context) {
        val preferences =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE,
            )

        val savedMode =
            preferences.getInt(
                KEY_NIGHT_MODE,
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            )

        if (AppCompatDelegate.getDefaultNightMode() != savedMode) {
            AppCompatDelegate.setDefaultNightMode(savedMode)
        }
    }

    fun isDarkMode(context: Context): Boolean {
        val currentMode = context.resources.configuration.uiMode
        return (currentMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }

    fun toggle(context: Context) {
        val newMode =
            if (isDarkMode(context)) {
                AppCompatDelegate.MODE_NIGHT_NO
            } else {
                AppCompatDelegate.MODE_NIGHT_YES
            }

        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE,
            ).edit()
            .putInt(KEY_NIGHT_MODE, newMode)
            .apply()

        AppCompatDelegate.setDefaultNightMode(newMode)
    }
}

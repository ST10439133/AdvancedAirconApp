package com.prog7314.arcticflow.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

class LocaleManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "locale_prefs"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_COUNTRY = "country"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getCurrentLocale(): Locale {
        val language = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        val country = prefs.getString(KEY_COUNTRY, "ZA") ?: "ZA"
        return Locale(language, country)
    }

    fun setLocale(locale: Locale) {
        prefs.edit().apply {
            putString(KEY_LANGUAGE, locale.language)
            putString(KEY_COUNTRY, locale.country)
            apply()
        }
        applyLocale(locale)
    }

    private fun applyLocale(locale: Locale) {
        val resources: Resources = context.resources
        val configuration: Configuration = resources.configuration
        configuration.setLocale(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    fun getAvailableLocales(): List<Locale> {
        return listOf(
            Locale("en", "ZA"), // English (South Africa)
            Locale("en", "US"), // English (US)
            Locale("af", "ZA"), // Afrikaans
            Locale("zu", "ZA")  // Zulu
        )
    }
}
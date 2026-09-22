package com.insy7315.advancedaircornapp.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

class LocaleManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "locale_prefs"
        private const val KEY_LANGUAGE = "language"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getCurrentLocale(): Locale {
        val language = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        return Locale(language)
    }

    fun setLocale(locale: Locale) {
        prefs.edit().putString(KEY_LANGUAGE, locale.language).apply()
        applyLocale(locale)
    }

    private fun applyLocale(locale: Locale) {
        val resources: Resources = context.resources
        val configuration: Configuration = resources.configuration
        configuration.setLocale(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    fun getAvailableLocales(): List<LocaleInfo> {
        return listOf(
            LocaleInfo(Locale("en"), "English", "🇬🇧"),
            LocaleInfo(Locale("af"), "Afrikaans", "🇿🇦"),
            LocaleInfo(Locale("zu"), "isiZulu", "🇿🇦")
        )
    }
}

data class LocaleInfo(
    val locale: Locale,
    val displayName: String,
    val flag: String = ""
)

// All translations in one place
object Translations {
    private val translations = mapOf(
        "en" to mapOf(
            "app_name" to "ArcticFlow",
            "settings" to "Settings",
            "profile" to "Profile",
            "edit_profile" to "Edit Profile",
            "sign_out" to "Sign Out",
            "dark_mode" to "Dark Mode",
            "dynamic_colors" to "Dynamic Colors (Android 12+)",
            "language" to "Language",
            "change_language" to "Change Language",
            "account" to "Account",
            "appearance" to "Appearance",
            "view_profile" to "View Profile",
            "welcome_back" to "Welcome Back",
            "sign_in_to_continue" to "Sign in to continue",
            "email" to "Email",
            "password" to "Password",
            "forgot_password" to "Forgot Password?",
            "sign_in" to "Sign In",
            "or" to "OR",
            "sign_in_with_google" to "Sign in with Google",
            "dont_have_account" to "Don't have an account?",
            "register" to "Register",
            "create_account" to "Create Account",
            "register_to_get_started" to "Register to get started",
            "full_name" to "Full Name",
            "confirm_password" to "Confirm Password",
            "select_your_role" to "Select Your Role",
            "technician" to "Technician",
            "manager" to "Manager",
            "already_have_account" to "Already have an account?",
            "search_products" to "Search products...",
            "all_brands" to "All Brands",
            "filter_by_brand" to "Filter by Brand",
            "a_z" to "A-Z",
            "price_low" to "Price: Low",
            "price_high" to "Price: High",
            "rating" to "Rating",
            "btu" to "BTU",
            "loading" to "Loading...",
            "loading_timeout" to "Loading is taking too long...",
            "name" to "Name",
            "role" to "Role",
            "account_created" to "Account Created",
            "email_verified" to "Email Verified",
            "yes" to "Yes",
            "no" to "No",
            "not_set" to "Not set",
            "save" to "Save",
            "cancel" to "Cancel",
            "select_language" to "Select Language"
        ),
        "af" to mapOf(
            "app_name" to "ArcticFlow",
            "settings" to "Instellings",
            "profile" to "Profiel",
            "edit_profile" to "Wysig Profiel",
            "sign_out" to "Teken Uit",
            "dark_mode" to "Donker Modus",
            "dynamic_colors" to "Dinamiese Kleure (Android 12+)",
            "language" to "Taal",
            "change_language" to "Verander Taal",
            "account" to "Rekening",
            "appearance" to "Voorkoms",
            "view_profile" to "Bekyk Profiel",
            "welcome_back" to "Welkom Terug",
            "sign_in_to_continue" to "Teken in om voort te gaan",
            "email" to "E-pos",
            "password" to "Wagwoord",
            "forgot_password" to "Vergeet Wagwoord?",
            "sign_in" to "Teken In",
            "or" to "OF",
            "sign_in_with_google" to "Teken in met Google",
            "dont_have_account" to "Het jy nie 'n rekening nie?",
            "register" to "Registreer",
            "create_account" to "Skep Rekening",
            "register_to_get_started" to "Registreer om te begin",
            "full_name" to "Volle Naam",
            "confirm_password" to "Bevestig Wagwoord",
            "select_your_role" to "Kies Jou Rol",
            "technician" to "Tegnikus",
            "manager" to "Bestuurder",
            "already_have_account" to "Het jy al 'n rekening?",
            "search_products" to "Soek produkte...",
            "all_brands" to "Alle Handelsmerke",
            "filter_by_brand" to "Filter volgens Handelsmerk",
            "a_z" to "A-Z",
            "price_low" to "Prys: Laag",
            "price_high" to "Prys: Hoog",
            "rating" to "Gradering",
            "btu" to "BTU",
            "loading" to "Laai...",
            "loading_timeout" to "Laai neem te lank...",
            "name" to "Naam",
            "role" to "Rol",
            "account_created" to "Rekening Geskep",
            "email_verified" to "E-pos Geverifieer",
            "yes" to "Ja",
            "no" to "Nee",
            "not_set" to "Nie gestel nie",
            "save" to "Stoor",
            "cancel" to "Kanselleer",
            "select_language" to "Kies Taal"
        ),
        "zu" to mapOf(
            "app_name" to "ArcticFlow",
            "settings" to "Izilungiselelo",
            "profile" to "Iphrofayili",
            "edit_profile" to "Hlela Iphrofayili",
            "sign_out" to "Phuma",
            "dark_mode" to "Imodi Emnyama",
            "dynamic_colors" to "Imibala Eguqukayo (Android 12+)",
            "language" to "Ulimi",
            "change_language" to "Shintsha Ulimi",
            "account" to "I-akhawunti",
            "appearance" to "Ukubukeka",
            "view_profile" to "Buka Iphrofayili",
            "welcome_back" to "Siyakwamukela",
            "sign_in_to_continue" to "Ngena ukuze uqhubeke",
            "email" to "I-imeyili",
            "password" to "Iphasiwedi",
            "forgot_password" to "Ukhohliwe Iphasiwedi?",
            "sign_in" to "Ngena",
            "or" to "NOMA",
            "sign_in_with_google" to "Ngena nge-Google",
            "dont_have_account" to "Awunayo i-akhawunti?",
            "register" to "Bhalisa",
            "create_account" to "Dala I-akhawunti",
            "register_to_get_started" to "Bhalisa ukuze uqale",
            "full_name" to "Igazi Eliphelele",
            "confirm_password" to "Qinisekisa Iphasiwedi",
            "select_your_role" to "Khetha Indima Yakho",
            "technician" to "Uchwepheshe",
            "manager" to "Umphathi",
            "already_have_account" to "Usenayo i-akhawunti?",
            "search_products" to "Sesha imikhiqizo...",
            "all_brands" to "Zonke Izimpawu Zomkhiqizo",
            "filter_by_brand" to "Hlunga Ngomkhiqizo",
            "a_z" to "A-Z",
            "price_low" to "Intengo: Ephansi",
            "price_high" to "Intengo: Ephezulu",
            "rating" to "Isilinganiso",
            "btu" to "BTU",
            "loading" to "Iyalayisha...",
            "loading_timeout" to "Ukulayisha kuthatha isikhathi eside...",
            "name" to "Igama",
            "role" to "Indima",
            "account_created" to "I-akhawunti Idalwe",
            "email_verified" to "I-imeyili Iqinisekisiwe",
            "yes" to "Yebo",
            "no" to "Cha",
            "not_set" to "Akukasethwa",
            "save" to "Gcina",
            "cancel" to "Khansela",
            "select_language" to "Khetha Ulimi"
        )
    )

    fun getString(key: String, language: String = "en"): String {
        val translationMap = translations[language] ?: translations["en"] ?: emptyMap()
        return translationMap[key] ?: key
    }
}

// Extension function for Composable usage
@Composable
fun rememberTranslation(): (String) -> String {
    val context = LocalContext.current
    val localeManager = remember { LocaleManager(context) }
    val language = localeManager.getCurrentLocale().language
    return { key -> Translations.getString(key, language) }
}
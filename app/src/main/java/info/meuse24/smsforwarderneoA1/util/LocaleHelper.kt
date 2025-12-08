package info.meuse24.smsforwarderneoA1.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Utility for managing app locale and language settings
 */
object LocaleHelper {
    /**
     * Sets the app locale to the specified language code
     * @param context The context to use
     * @param languageCode The language code (e.g., "en", "de")
     * @return A new context with the updated locale
     */
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }

    /**
     * Wraps the context with the specified language code, or returns unchanged if null
     * @param context The context to wrap
     * @param languageCode The language code, or null for system default
     * @return The wrapped context with custom locale, or original context if languageCode is null
     */
    fun wrapContext(context: Context, languageCode: String?): Context {
        return if (languageCode != null) {
            setLocale(context, languageCode)
        } else {
            context
        }
    }

    /**
     * Gets the current language code from preferences
     * @param context The context to use
     * @return The language code or null for system default
     */
    fun getCurrentLanguage(context: Context): String? {
        val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        return prefs.getString("app_language", null)
    }
}

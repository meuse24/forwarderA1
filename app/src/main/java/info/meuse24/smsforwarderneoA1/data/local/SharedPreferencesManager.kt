package info.meuse24.smsforwarderneoA1.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import info.meuse24.smsforwarderneoA1.LoggingManager
import info.meuse24.smsforwarderneoA1.domain.model.MmiSimSelectionMode
import info.meuse24.smsforwarderneoA1.domain.model.SimSelectionMode
import java.io.File

/**
 * Encrypted SharedPreferences manager for app settings.
 *
 * Features:
 * - Encrypted storage using androidx.security.crypto
 * - Type-safe preference access (String, Boolean, Int, List)
 * - Validation and migration support
 * - Fallback to unencrypted storage on failure
 */
class SharedPreferencesManager(private val context: Context) {
    private val prefs: SharedPreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        initializePreferences()
    }

    private fun <T> getPreference(key: String, defaultValue: T): T {
        return try {
            @Suppress("UNCHECKED_CAST")
            when (defaultValue) {
                is String -> (prefs.getString(key, defaultValue) ?: defaultValue) as T
                is Boolean -> (prefs.getBoolean(key, defaultValue)) as T
                is Int -> (prefs.getInt(key, defaultValue)) as T
                is List<*> -> {
                    val value = prefs.getString(key, "")
                    if (value.isNullOrEmpty()) emptyList<String>() as T
                    else value.split(",").filter { it.isNotEmpty() } as T
                }
                else -> defaultValue
            }
        } catch (e: Exception) {
            LoggingManager.logError(
                component = "SharedPreferencesManager",
                action = "GET_PREFERENCE",
                message = "Fehler beim Lesen: $key",
                error = e
            )
            defaultValue
        }
    }

    private fun <T> setPreference(key: String, value: T) {
        try {
            prefs.edit().apply {
                when (value) {
                    is String -> putString(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is List<*> -> putString(key, value.joinToString(","))
                    null -> remove(key)
                }
                apply()
            }
        } catch (e: Exception) {
            LoggingManager.logError(
                component = "SharedPreferencesManager",
                action = "SET_PREFERENCE",
                message = "Fehler beim Speichern: $key",
                error = e
            )
        }
    }

    fun saveSelectedPhoneNumber(phoneNumber: String) {
        prefs.edit().apply {
            putString(KEY_SELECTED_PHONE, phoneNumber)
            putBoolean(KEY_FORWARDING_ACTIVE, true)
            apply()
        }

        LoggingManager.logInfo(
            component = "SharedPreferencesManager",
            action = "SAVE_PHONE_NUMBER",
            message = "Zielrufnummer aktualisiert",
            details = mapOf(
                "number" to phoneNumber,
                "forwarding_active" to isForwardingActive()
            )
        )
    }

    // Aktiviere Weiterleitung mit Telefonnummer
    private fun activateForwarding(phoneNumber: String) {
        require(phoneNumber.isNotEmpty()) { "Telefonnummer darf nicht leer sein" }
        prefs.edit().apply {
            putBoolean(KEY_FORWARDING_ACTIVE, true)
            putString(KEY_SELECTED_PHONE, phoneNumber)
            apply()
        }
        LoggingManager.logInfo(
            component = "SharedPreferencesManager",
            action = "STORE_ACTIVATE_FORWARDING",
            message = "Weiterleitung-Aktivierung gespeichert",
            details = mapOf("number" to phoneNumber)
        )
    }

    // Deaktiviere Weiterleitung
    private fun deactivateForwarding() {
        prefs.edit().apply {
            putBoolean(KEY_FORWARDING_ACTIVE, false)
            putString(KEY_SELECTED_PHONE, "")
            apply()
        }
        LoggingManager.logInfo(
            component = "SharedPreferencesManager",
            action = "STORE_DEACTIVATE_FORWARDING",
            message = "Weiterleitung-Deaktivierung gespeichert"
        )
    }

    init {
        validateForwardingState()
        migrateOldPreferences()
    }

    // Prüfe ob Weiterleitung aktiv ist
    fun isForwardingActive(): Boolean =
        prefs.getBoolean(KEY_FORWARDING_ACTIVE, false)

    // Keep Forwarding on Exit Funktionen
    fun setKeepForwardingOnExit(keep: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_FORWARDING_ON_EXIT, keep).apply()
    }

    fun getKeepForwardingOnExit(): Boolean =
        prefs.getBoolean(KEY_KEEP_FORWARDING_ON_EXIT, false)

    // Validiere und repariere inkonsistente Zustände
    private fun validateForwardingState() {
        val isActive = prefs.getBoolean(KEY_FORWARDING_ACTIVE, false)
        val number = prefs.getString(KEY_SELECTED_PHONE, "") ?: ""

        when {
            // Aktiv aber keine Nummer
            isActive && number.isEmpty() -> {
                deactivateForwarding()
                LoggingManager.logWarning(
                    component = "SharedPreferencesManager",
                    action = "VALIDATE_STATE",
                    message = "Inkonsistenter Status korrigiert: Aktiv ohne Nummer"
                )
            }
            // Inaktiv aber Nummer vorhanden
            !isActive && number.isNotEmpty() -> {
                deactivateForwarding()
                LoggingManager.logWarning(
                    component = "SharedPreferencesManager",
                    action = "VALIDATE_STATE",
                    message = "Inkonsistenter Status korrigiert: Inaktiv mit Nummer"
                )
            }
        }
    }

    // Migriere alte Präferenzen falls nötig
    private fun migrateOldPreferences() {
        try {
            // Beispiel für Migration von alten Keys
            if (prefs.contains("old_forwarding_number")) {
                val oldNumber = prefs.getString("old_forwarding_number", "") ?: ""
                val oldActive = prefs.getBoolean("old_forwarding_status", false)

                if (oldActive && oldNumber.isNotEmpty()) {
                    activateForwarding(oldNumber)
                } else {
                    deactivateForwarding()
                }

                // Lösche alte Keys
                prefs.edit().apply {
                    remove("old_forwarding_number")
                    remove("old_forwarding_status")
                    apply()
                }

                LoggingManager.logInfo(
                    component = "SharedPreferencesManager",
                    action = "MIGRATE_PREFS",
                    message = "Alte Präferenzen migriert"
                )
            }
        } catch (e: Exception) {
            LoggingManager.logError(
                component = "SharedPreferencesManager",
                action = "MIGRATE_ERROR",
                message = "Fehler bei der Migration",
                error = e
            )
            // Bei Fehler sicheren Zustand herstellen
            deactivateForwarding()
        }
    }

    fun saveForwardingStatus(isActive: Boolean) =
        setPreference(KEY_FORWARDING_ACTIVE, isActive)

    fun getSelectedPhoneNumber(): String =
        getPreference(KEY_SELECTED_PHONE, "")

    fun saveContactName(name: String) {
        setPreference(KEY_CONTACT_NAME, name)

        LoggingManager.logInfo(
            component = "SharedPreferencesManager",
            action = "SAVE_CONTACT_NAME",
            message = "Kontaktname gespeichert",
            details = mapOf("name" to name)
        )
    }

    fun getContactName(): String =
        getPreference(KEY_CONTACT_NAME, "")

    fun clearSelection() {
        prefs.edit().apply {
            setPreference(KEY_SELECTED_PHONE, "")
            setPreference(KEY_CONTACT_NAME, "")
            putBoolean(KEY_FORWARDING_ACTIVE, false)
            apply()
        }

        LoggingManager.logInfo(
            component = "SharedPreferencesManager",
            action = "CLEAR_SELECTION",
            message = "Weiterleitung und Zielrufnummer zurückgesetzt"
        )
    }

    fun saveTestEmailText(text: String) =
        setPreference(KEY_TEST_EMAIL_TEXT, text)

    fun isForwardSmsToEmail(): Boolean =
        getPreference(KEY_FORWARD_SMS_TO_EMAIL, false)

    fun setForwardSmsToEmail(enabled: Boolean) =
        setPreference(KEY_FORWARD_SMS_TO_EMAIL, enabled)

    fun getEmailAddresses(): List<String> =
        getPreference(KEY_EMAIL_ADDRESSES, emptyList())

    fun saveEmailAddresses(emails: List<String>) =
        setPreference(KEY_EMAIL_ADDRESSES, emails)

    fun saveCountryCode(code: String) {
        if (isValidCountryCode(code)) {
            setPreference(KEY_COUNTRY_CODE, code)
        }
    }

    fun saveTestSmsText(text: String) =
        setPreference(KEY_TEST_SMS_TEXT, text)

    fun getSmtpUsername(): String =
        getPreference(KEY_SMTP_USERNAME, "")

    fun getSmtpPassword(): String =
        getPreference(KEY_SMTP_PASSWORD, "")

    fun saveSmtpSettings(host: String, port: Int, username: String, password: String) {
        prefs.edit().apply {
            putString(KEY_SMTP_HOST, host)
            putInt(KEY_SMTP_PORT, port)
            putString(KEY_SMTP_USERNAME, username)
            putString(KEY_SMTP_PASSWORD, password)
            apply()
        }
    }

    private fun isValidCountryCode(code: String): Boolean =
        code in setOf("+43", "+49", "+41")

    private fun initializePreferences(): SharedPreferences {
        return try {
            createEncryptedPreferences()
        } catch (e: Exception) {
            handlePreferencesError(e)
            // SECURITY: Do NOT fallback to unencrypted preferences
            // Instead, rethrow the exception to prevent app from running with compromised security
            throw PreferencesInitializationException(
                "Verschlüsselte Datenspeicherung konnte nicht initialisiert werden. " +
                "Bitte App-Daten löschen und neu starten.",
                e
            )
        }
    }

    private fun createEncryptedPreferences(): SharedPreferences {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            return EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            throw PreferencesInitializationException("Failed to create encrypted preferences", e)
        }
    }

    // REMOVED: createUnencryptedPreferences() - no longer needed, no plaintext fallback for security

    private fun handlePreferencesError(error: Exception) {
        LoggingManager.logError(
            component = "SharedPreferencesManager",
            action = "INIT_ERROR",
            message = "SharedPreferences Initialisierungsfehler",
            error = error,
            details = mapOf(
                "error_type" to error.javaClass.simpleName,
                "error_message" to (error.message ?: "Unknown error")
            )
        )

        try {
            val prefsFile =
                File(context.applicationInfo.dataDir + "/shared_prefs/" + PREFS_NAME + ".xml")
            if (prefsFile.exists()) {
                prefsFile.delete()
                LoggingManager.logInfo(
                    component = "SharedPreferencesManager",
                    action = "DELETE_CORRUPTED",
                    message = "Beschädigte Preferences gelöscht"
                )
            }
        } catch (e: Exception) {
            LoggingManager.logError(
                component = "SharedPreferencesManager",
                action = "DELETE_ERROR",
                message = "Fehler beim Löschen der beschädigten Preferences",
                error = e,
                details = mapOf("error" to e.message)
            )
        }
    }

    fun getTestSmsText(): String =
        getPreference(KEY_TEST_SMS_TEXT, DEFAULT_TEST_SMS_TEXT)

    fun getTestEmailText(): String =
        getPreference(KEY_TEST_EMAIL_TEXT, DEFAULT_TEST_EMAIL_TEXT)

    fun getSmtpHost(): String =
        getPreference(KEY_SMTP_HOST, DEFAULT_SMTP_HOST)

    fun getSmtpPort(): Int =
        getPreference(KEY_SMTP_PORT, DEFAULT_SMTP_PORT)

    // SIM-spezifische Telefonnummern-Verwaltung
    fun setSimPhoneNumbers(simNumbers: Map<Int, String>) {
        val json = simNumbers.entries.joinToString(";") { "${it.key}:${it.value}" }
        setPreference(KEY_SIM_PHONE_NUMBERS, json)
    }

    fun getSimPhoneNumbers(): Map<Int, String> {
        val json = getPreference(KEY_SIM_PHONE_NUMBERS, "")
        return if (json.isEmpty()) {
            emptyMap()
        } else {
            try {
                json.split(";")
                    .filter { it.contains(":") }
                    .associate {
                        val parts = it.split(":", limit = 2)
                        parts[0].toInt() to parts[1]
                    }
            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "SharedPreferencesManager",
                    action = "GET_SIM_PHONE_NUMBERS",
                    message = "Fehler beim Parsen der SIM-Nummern",
                    error = e
                )
                emptyMap()
            }
        }
    }

    fun setSimPhoneNumber(subscriptionId: Int, phoneNumber: String) {
        val current = getSimPhoneNumbers().toMutableMap()
        current[subscriptionId] = phoneNumber
        setSimPhoneNumbers(current)
    }

    fun getSimPhoneNumber(subscriptionId: Int): String? {
        return getSimPhoneNumbers()[subscriptionId]
    }

    fun removeSimPhoneNumber(subscriptionId: Int) {
        val current = getSimPhoneNumbers().toMutableMap()
        current.remove(subscriptionId)
        setSimPhoneNumbers(current)
    }

    fun getCountryCode(defaultCode: String = DEFAULT_COUNTRY_CODE): String =
        getPreference(KEY_COUNTRY_CODE, defaultCode)

    // Neue Methoden hinzufügen:
    fun setLogPIN(pin: String) =
        setPreference(KEY_LOG_PIN, pin)

    fun getLogPIN(): String =
        getPreference(KEY_LOG_PIN, "0000") // Default PIN ist 0000

    // Mail Screen Visibility Funktionen
    fun setMailScreenVisible(visible: Boolean) =
        setPreference(KEY_MAIL_SCREEN_VISIBLE, visible)

    fun isMailScreenVisible(): Boolean =
        getPreference(KEY_MAIL_SCREEN_VISIBLE, false) // standardmäßig ausgeblendet

    // MMI Code Funktionen
    fun setMmiActivatePrefix(prefix: String) =
        setPreference(KEY_MMI_ACTIVATE_PREFIX, prefix)

    fun getMmiActivatePrefix(): String =
        getPreference(KEY_MMI_ACTIVATE_PREFIX, DEFAULT_MMI_ACTIVATE_PREFIX)

    fun setMmiActivateSuffix(suffix: String) =
        setPreference(KEY_MMI_ACTIVATE_SUFFIX, suffix)

    fun getMmiActivateSuffix(): String =
        getPreference(KEY_MMI_ACTIVATE_SUFFIX, DEFAULT_MMI_ACTIVATE_SUFFIX)

    fun setMmiDeactivateCode(code: String) =
        setPreference(KEY_MMI_DEACTIVATE_CODE, code)

    fun getMmiDeactivateCode(): String =
        getPreference(KEY_MMI_DEACTIVATE_CODE, DEFAULT_MMI_DEACTIVATE_CODE)

    fun setMmiStatusCode(code: String) =
        setPreference(KEY_MMI_STATUS_CODE, code)

    fun getMmiStatusCode(): String =
        getPreference(KEY_MMI_STATUS_CODE, DEFAULT_MMI_STATUS_CODE)

    fun resetMmiCodesToDefault() {
        setMmiActivatePrefix(DEFAULT_MMI_ACTIVATE_PREFIX)
        setMmiActivateSuffix(DEFAULT_MMI_ACTIVATE_SUFFIX)
        setMmiDeactivateCode(DEFAULT_MMI_DEACTIVATE_CODE)
        setMmiStatusCode(DEFAULT_MMI_STATUS_CODE)
    }

    fun resetMmiCodesToGeneric() {
        setMmiActivatePrefix(GENERIC_MMI_ACTIVATE_PREFIX)
        setMmiActivateSuffix(GENERIC_MMI_ACTIVATE_SUFFIX)
        setMmiDeactivateCode(GENERIC_MMI_DEACTIVATE_CODE)
        setMmiStatusCode(GENERIC_MMI_STATUS_CODE)
    }

    /**
     * Speichert den SIM-Auswahl-Modus für SMS-Weiterleitung.
     * @param mode Der gewünschte SIM-Auswahl-Modus
     */
    fun setSimSelectionMode(mode: SimSelectionMode) {
        setPreference(KEY_SIM_SELECTION_MODE, mode.name)
        LoggingManager.logInfo(
            component = "SharedPreferencesManager",
            action = "SET_SIM_SELECTION_MODE",
            message = "SIM-Auswahl-Modus gespeichert",
            details = mapOf("mode" to mode.name)
        )
    }

    /**
     * Liest den gespeicherten SIM-Auswahl-Modus.
     * @return Der gespeicherte Modus oder SAME_AS_INCOMING als Standard
     */
    fun getSimSelectionMode(): SimSelectionMode {
        val value = getPreference(KEY_SIM_SELECTION_MODE, "")
        return SimSelectionMode.fromString(value)
    }

    /**
     * Speichert den SIM-Auswahl-Modus für MMI-Code-Ausführung.
     * @param mode Der gewünschte MMI-SIM-Auswahl-Modus
     */
    fun setMmiSimSelectionMode(mode: MmiSimSelectionMode) {
        setPreference(KEY_MMI_SIM_SELECTION_MODE, mode.name)
        LoggingManager.logInfo(
            component = "SharedPreferencesManager",
            action = "SET_MMI_SIM_SELECTION_MODE",
            message = "MMI SIM-Auswahl-Modus gespeichert",
            details = mapOf("mode" to mode.name)
        )
    }

    /**
     * Liest den gespeicherten MMI-SIM-Auswahl-Modus.
     * @return Der gespeicherte Modus oder DEFAULT_VOICE_SIM als Standard
     */
    fun getMmiSimSelectionMode(): MmiSimSelectionMode {
        val value = getPreference(KEY_MMI_SIM_SELECTION_MODE, "")
        return MmiSimSelectionMode.fromString(value)
    }

    /**
     * Speichert die internationale Anschaltziffernfolge.
     * @param prefix Die Anschaltziffernfolge (z.B. "00" für Österreich)
     */
    fun setInternationalDialPrefix(prefix: String) {
        setPreference(KEY_INTERNATIONAL_DIAL_PREFIX, prefix)
        LoggingManager.logInfo(
            component = "SharedPreferencesManager",
            action = "SET_INTERNATIONAL_DIAL_PREFIX",
            message = "Internationale Anschaltziffernfolge gespeichert",
            details = mapOf("prefix" to prefix)
        )
    }

    /**
     * Liest die gespeicherte internationale Anschaltziffernfolge.
     * @return Die gespeicherte Anschaltziffernfolge oder "00" als Standard
     */
    fun getInternationalDialPrefix(): String =
        getPreference(KEY_INTERNATIONAL_DIAL_PREFIX, DEFAULT_INTERNATIONAL_DIAL_PREFIX)

    /**
     * Aktiviert/deaktiviert die 4-Sekunden-Warnung vor MMI-Code-Wahl.
     * @param enabled true = Warnung anzeigen, false = Warnung überspringen
     */
    fun setMmiWarningEnabled(enabled: Boolean) =
        setPreference(KEY_MMI_WARNING_ENABLED, enabled)

    /**
     * Prüft, ob die MMI-Warnung aktiviert ist.
     * @return true wenn Warnung angezeigt werden soll (Standard), false zum Überspringen
     */
    fun isMmiWarningEnabled(): Boolean =
        getPreference(KEY_MMI_WARNING_ENABLED, DEFAULT_MMI_WARNING_ENABLED)

    /**
     * Setzt die maximale Log-Dateigröße in MB.
     * @param sizeMB Größe in MB (1-20, Standard: 5)
     */
    fun setMaxLogSizeMB(sizeMB: Int) {
        val validSize = sizeMB.coerceIn(1, 20)
        setPreference(KEY_MAX_LOG_SIZE_MB, validSize)
    }

    /**
     * Liest die maximale Log-Dateigröße in MB.
     * @return Größe in MB (Standard: 5)
     */
    fun getMaxLogSizeMB(): Int =
        getPreference(KEY_MAX_LOG_SIZE_MB, DEFAULT_MAX_LOG_SIZE_MB)

    /**
     * Speichert, ob die Datenschutzerklärung akzeptiert wurde.
     * @param accepted true wenn akzeptiert, false wenn abgelehnt
     */
    fun setPrivacyPolicyAccepted(accepted: Boolean) =
        setPreference(KEY_PRIVACY_POLICY_ACCEPTED, accepted)

    /**
     * Prüft, ob die Datenschutzerklärung akzeptiert wurde.
     * @return true wenn akzeptiert, false wenn noch nicht angezeigt/abgelehnt
     */
    fun isPrivacyPolicyAccepted(): Boolean =
        getPreference(KEY_PRIVACY_POLICY_ACCEPTED, false)

    /**
     * Aktiviert/deaktiviert SMS-Empfang von SIM 1.
     * @param enabled true = Empfangen aktiviert (Standard), false = Empfang deaktiviert
     */
    fun setSim1ReceiveEnabled(enabled: Boolean) =
        setPreference(KEY_SIM1_RECEIVE_ENABLED, enabled)

    /**
     * Prüft, ob SMS-Empfang von SIM 1 aktiviert ist.
     * @return true wenn aktiviert (Standard), false wenn deaktiviert
     */
    fun isSim1ReceiveEnabled(): Boolean =
        getPreference(KEY_SIM1_RECEIVE_ENABLED, true)

    /**
     * Aktiviert/deaktiviert SMS-Empfang von SIM 2.
     * @param enabled true = Empfangen aktiviert (Standard), false = Empfang deaktiviert
     */
    fun setSim2ReceiveEnabled(enabled: Boolean) =
        setPreference(KEY_SIM2_RECEIVE_ENABLED, enabled)

    /**
     * Prüft, ob SMS-Empfang von SIM 2 aktiviert ist.
     * @return true wenn aktiviert (Standard), false wenn deaktiviert
     */
    fun isSim2ReceiveEnabled(): Boolean =
        getPreference(KEY_SIM2_RECEIVE_ENABLED, true)

    /**
     * Speichert die ausgewählte App-Sprache.
     * WICHTIG: Wird in normalen (unverschlüsselten) Preferences gespeichert,
     * da sie in attachBaseContext() gelesen werden muss (vor verschlüsselter Initialisierung).
     * @param languageCode Der Sprachcode (z.B. "en", "de") oder null für Systemsprache
     */
    fun setAppLanguage(languageCode: String?) {
        // Speichere in NORMALEN Preferences (nicht verschlüsselt) für frühen Zugriff
        val plainPrefs = context.getSharedPreferences("app_language_prefs", android.content.Context.MODE_PRIVATE)
        if (languageCode == null) {
            plainPrefs.edit().remove(KEY_APP_LANGUAGE).apply()
        } else {
            plainPrefs.edit().putString(KEY_APP_LANGUAGE, languageCode).apply()
        }

        LoggingManager.logInfo(
            component = "SharedPreferencesManager",
            action = "SET_APP_LANGUAGE",
            message = "App-Sprache geändert",
            details = mapOf("language" to (languageCode ?: "system"))
        )
    }

    /**
     * Liest die ausgewählte App-Sprache.
     * @return Der Sprachcode oder null für Systemsprache (Standard)
     */
    fun getAppLanguage(): String? {
        // Lese aus normalen Preferences (nicht verschlüsselt)
        val plainPrefs = context.getSharedPreferences("app_language_prefs", android.content.Context.MODE_PRIVATE)
        return plainPrefs.getString(KEY_APP_LANGUAGE, null)
    }

    companion object {
        private const val KEY_TEST_EMAIL_TEXT = "test_email_text"
        private const val KEY_FORWARD_SMS_TO_EMAIL = "forward_sms_to_email"
        private const val KEY_EMAIL_ADDRESSES = "email_addresses"
        private const val KEY_LOG_PIN = "log_pin"
        private const val KEY_TEST_SMS_TEXT = "test_sms_text"
        private const val KEY_SIM_PHONE_NUMBERS = "sim_phone_numbers"
        private const val KEY_COUNTRY_CODE = "country_code"
        private const val KEY_SMTP_HOST = "smtp_host"
        private const val KEY_SMTP_PORT = "smtp_port"
        private const val KEY_SMTP_USERNAME = "smtp_username"
        private const val KEY_SMTP_PASSWORD = "smtp_password"
        private const val DEFAULT_SMTP_HOST = "smtp.gmail.com"
        private const val DEFAULT_SMTP_PORT = 587
        private const val PREFS_NAME = "sms_forwarder_secure_prefs"
        // REMOVED: PREFS_NAME_FALLBACK - no plaintext fallback for security
        private const val DEFAULT_TEST_SMS_TEXT = "Test: GSM-7 Extended Chars -> {€}[^]|~\\ <- prüft korrekte Kodierung (2 Septets je Zeichen)"
        private const val DEFAULT_TEST_EMAIL_TEXT = "Das ist eine Test-Email"
        private const val DEFAULT_COUNTRY_CODE = "+43"
        private const val KEY_FORWARDING_ACTIVE = "forwarding_active"
        private const val KEY_SELECTED_PHONE = "selected_phone_number"
        private const val KEY_CONTACT_NAME = "contact_name"
        private const val KEY_KEEP_FORWARDING_ON_EXIT = "keep_forwarding_on_exit"
        private const val KEY_MAIL_SCREEN_VISIBLE = "mail_screen_visible"
        private const val KEY_MMI_ACTIVATE_PREFIX = "mmi_activate_prefix"
        private const val KEY_MMI_ACTIVATE_SUFFIX = "mmi_activate_suffix"
        private const val KEY_MMI_DEACTIVATE_CODE = "mmi_deactivate_code"
        private const val KEY_MMI_STATUS_CODE = "mmi_status_code"
        private const val KEY_SIM_SELECTION_MODE = "sim_selection_mode"
        private const val KEY_MMI_SIM_SELECTION_MODE = "mmi_sim_selection_mode"
        private const val KEY_INTERNATIONAL_DIAL_PREFIX = "international_dial_prefix"
        private const val KEY_MMI_WARNING_ENABLED = "mmi_warning_enabled"
        private const val KEY_MAX_LOG_SIZE_MB = "max_log_size_mb"
        private const val KEY_PRIVACY_POLICY_ACCEPTED = "privacy_policy_accepted"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_SIM1_RECEIVE_ENABLED = "sim1_receive_enabled"
        private const val KEY_SIM2_RECEIVE_ENABLED = "sim2_receive_enabled"

        // BMI/A1 Codes (New Defaults)
        private const val DEFAULT_MMI_ACTIVATE_PREFIX = "*21*"
        private const val DEFAULT_MMI_ACTIVATE_SUFFIX = "**"
        private const val DEFAULT_MMI_DEACTIVATE_CODE = "**21**"
        private const val DEFAULT_MMI_STATUS_CODE = "*021**"

        // Generic Standard Codes (Old Defaults)
        private const val GENERIC_MMI_ACTIVATE_PREFIX = "*21*"
        private const val GENERIC_MMI_ACTIVATE_SUFFIX = "#"
        private const val GENERIC_MMI_DEACTIVATE_CODE = "##21#"
        private const val GENERIC_MMI_STATUS_CODE = "*#21#"

        // International Dial Prefix (Default für Österreich)
        private const val DEFAULT_INTERNATIONAL_DIAL_PREFIX = "00"

        // MMI Warning (Default: enabled)
        private const val DEFAULT_MMI_WARNING_ENABLED = true

        // Log Settings (Default: 5MB)
        private const val DEFAULT_MAX_LOG_SIZE_MB = 5
    }
}

class PreferencesInitializationException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

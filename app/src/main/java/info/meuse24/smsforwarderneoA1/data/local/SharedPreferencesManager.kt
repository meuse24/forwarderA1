package info.meuse24.smsforwarderneoA1.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import info.meuse24.smsforwarderneoA1.LoggingManager
import info.meuse24.smsforwarderneoA1.domain.model.PersistedMmiOperation
import info.meuse24.smsforwarderneoA1.domain.model.MmiOperationState
import info.meuse24.smsforwarderneoA1.domain.model.MmiEvidence
import info.meuse24.smsforwarderneoA1.domain.model.MmiSimSelectionMode
import info.meuse24.smsforwarderneoA1.domain.model.MmiExecutionMode
import info.meuse24.smsforwarderneoA1.domain.model.ForwardingVerification
import info.meuse24.smsforwarderneoA1.domain.model.DialPath
import info.meuse24.smsforwarderneoA1.domain.model.MmiAuditEntry
import info.meuse24.smsforwarderneoA1.domain.model.MmiAuditRetentionPolicy
import info.meuse24.smsforwarderneoA1.domain.model.SimSelectionMode
import info.meuse24.smsforwarderneoA1.domain.model.MmiCodeProfile
import info.meuse24.smsforwarderneoA1.domain.model.MmiCodeProfiles
import info.meuse24.smsforwarderneoA1.domain.model.MmiCodeSet
import info.meuse24.smsforwarderneoA1.domain.model.ForwardingCodeSnapshot
import info.meuse24.smsforwarderneoA1.domain.model.MmiProfileMigration
import info.meuse24.smsforwarderneoA1.domain.model.DroppedForwardingWarning
import info.meuse24.smsforwarderneoA1.domain.model.QueueCorruptionWarning
import info.meuse24.smsforwarderneoA1.util.MmiCodeMasker
import java.io.File
import org.json.JSONObject
import org.json.JSONArray

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
                "number" to MmiCodeMasker.maskNumber(phoneNumber),
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
            details = mapOf("number" to MmiCodeMasker.maskNumber(phoneNumber))
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
        migrateMmiCodeProfile()
        validateForwardingState()
        migrateOldPreferences()
        removeLegacyMmiWarningPreference()
        pruneMmiAudit()
    }

    /**
     * Materializes legacy implicit defaults before their value is changed. Without this,
     * an update would silently alter installations which never opened the code settings.
     */
    private fun migrateMmiCodeProfile() {
        try {
        if (prefs.getInt(KEY_MMI_PROFILE_MIGRATION_VERSION, 0) >= MMI_PROFILE_MIGRATION_VERSION) return
        val mmiKeys = listOf(KEY_MMI_ACTIVATE_PREFIX, KEY_MMI_ACTIVATE_SUFFIX, KEY_MMI_DEACTIVATE_CODE, KEY_MMI_STATUS_CODE)
        val isUpdate = runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).let { it.firstInstallTime < it.lastUpdateTime }
        }.getOrDefault(false)
        val fallback = MmiProfileMigration.defaultsFor(
            hasAnyMmiKey = mmiKeys.any(prefs::contains),
            isUpdate = isUpdate,
            forwardingActive = prefs.getBoolean(KEY_FORWARDING_ACTIVE, false),
        )
        val codes = MmiProfileMigration.materialize(MmiCodeSet(
            prefs.getString(KEY_MMI_ACTIVATE_PREFIX, fallback.activatePrefix) ?: fallback.activatePrefix,
            prefs.getString(KEY_MMI_ACTIVATE_SUFFIX, fallback.activateSuffix) ?: fallback.activateSuffix,
            prefs.getString(KEY_MMI_DEACTIVATE_CODE, fallback.deactivateCode) ?: fallback.deactivateCode,
            prefs.getString(KEY_MMI_STATUS_CODE, fallback.statusCode) ?: fallback.statusCode,
        ), fallback)
        prefs.edit()
            .putString(KEY_MMI_ACTIVATE_PREFIX, codes.activatePrefix)
            .putString(KEY_MMI_ACTIVATE_SUFFIX, codes.activateSuffix)
            .putString(KEY_MMI_DEACTIVATE_CODE, codes.deactivateCode)
            .putString(KEY_MMI_STATUS_CODE, codes.statusCode)
            .putString(KEY_MMI_CODE_PROFILE, MmiCodeProfiles.detect(codes).name)
            .putInt(KEY_MMI_PROFILE_MIGRATION_VERSION, MMI_PROFILE_MIGRATION_VERSION)
            .apply()
        LoggingManager.logInfo(
            component = "SharedPreferencesManager",
            action = "MIGRATE_MMI_PROFILE",
            message = "MMI-Profil materialisiert",
            details = mapOf("profile" to MmiCodeProfiles.detect(codes).name, "is_update" to isUpdate)
        )
        if (prefs.getBoolean(KEY_FORWARDING_ACTIVE, false) && getForwardingCodeSnapshot() == null) {
            saveForwardingCodeSnapshot(
                ForwardingCodeSnapshot(
                    deactivateCode = codes.deactivateCode,
                    executionMode = when (MmiCodeProfiles.detect(codes)) {
                        MmiCodeProfile.STANDARD_GSM -> MmiExecutionMode.USSD_CALLBACK
                        MmiCodeProfile.A1_SPECIAL -> MmiExecutionMode.VOICE_MMI_CALL
                        MmiCodeProfile.CUSTOM -> getMmiExecutionMode(codes.deactivateCode)
                    },
                    subscriptionId = null,
                )
            )
        }
        } catch (e: Exception) {
            LoggingManager.logError(
                component = "SharedPreferencesManager",
                action = "MIGRATE_MMI_PROFILE_FAILED",
                message = "MMI-Profil-Migration fehlgeschlagen; bestehende Werte bleiben unverändert",
                error = e
            )
        }
    }

    /** Removes the no-longer-used pre-dial warning setting from existing installs. */
    private fun removeLegacyMmiWarningPreference() {
        prefs.edit().remove("mmi_warning_enabled").apply()
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

    /** Classifies a code through the configured MMI profile, not a global suffix heuristic. */
    fun getMmiExecutionMode(code: String): MmiExecutionMode {
        val configuredEnding = when {
            code == getMmiDeactivateCode() -> getMmiDeactivateCode().takeLast(1)
            code == getMmiStatusCode() -> getMmiStatusCode().takeLast(1)
            getMmiActivatePrefix().isNotBlank() && getMmiActivateSuffix().isNotBlank() &&
                code.startsWith(getMmiActivatePrefix()) && code.endsWith(getMmiActivateSuffix()) -> getMmiActivateSuffix().takeLast(1)
            else -> null
        }
        return if (configuredEnding == "#") MmiExecutionMode.USSD_CALLBACK else MmiExecutionMode.VOICE_MMI_CALL
    }

    fun saveForwardingVerification(value: ForwardingVerification) =
        setPreference(KEY_FORWARDING_VERIFICATION, value.name)

    fun getForwardingVerification(): ForwardingVerification =
        getPreference(KEY_FORWARDING_VERIFICATION, ForwardingVerification.NOT_CHECKED.name)
            .let { runCatching { ForwardingVerification.valueOf(it) }.getOrDefault(ForwardingVerification.NOT_CHECKED) }

    fun saveForwardingCodeSnapshot(snapshot: ForwardingCodeSnapshot) {
        setPreference(KEY_FORWARDING_CODE_SNAPSHOT, JSONObject().apply {
            put("deactivate_code", snapshot.deactivateCode)
            put("mode", snapshot.executionMode.name)
            put("subscription_id", snapshot.subscriptionId ?: -1)
        }.toString())
    }

    fun getForwardingCodeSnapshot(): ForwardingCodeSnapshot? = runCatching {
        val raw = getPreference(KEY_FORWARDING_CODE_SNAPSHOT, "")
        if (raw.isBlank()) return null
        JSONObject(raw).let { json ->
            ForwardingCodeSnapshot(
                json.getString("deactivate_code"),
                MmiExecutionMode.valueOf(json.getString("mode")),
                json.optInt("subscription_id", -1).takeIf { it >= 0 },
            )
        }
    }.getOrNull()

    fun clearForwardingCodeSnapshot() = setPreference(KEY_FORWARDING_CODE_SNAPSHOT, "")

    fun savePendingMmiRequest(request: PersistedMmiOperation) {
        val json = JSONObject().apply {
            put("id", request.id); put("action", request.action); put("code", request.code)
            put("mode", request.mode.name); put("dialed_at", request.dialedAtMillis)
            put("state", request.state.name); put("verification", request.verification.name)
            put("call_observed", request.evidence.callObserved); put("call_duration", request.evidence.callDurationMs)
            put("watchdog_expired", request.evidence.watchdogExpired); put("ussd_response", request.evidence.ussdResponse)
            put("contact_name", request.contactName); put("contact_number", request.contactNumber); put("contact_description", request.contactDescription)
            put("target_subscription_id", request.targetSubscriptionId); put("dial_path", request.dialPath.name)
            put("user_message", request.userMessage)
        }
        setPreference(KEY_PENDING_MMI_OPERATION, json.toString())
    }

    fun getPendingMmiRequest(): PersistedMmiOperation? = runCatching {
        val raw = getPreference(KEY_PENDING_MMI_OPERATION, "")
        if (raw.isBlank()) return null
        val json = JSONObject(raw)
        val number = json.optString("contact_number").takeIf { it.isNotBlank() }
        PersistedMmiOperation(json.getString("id"), json.getString("action"), json.getString("code"), MmiExecutionMode.valueOf(json.getString("mode")), json.getLong("dialed_at"), json.optString("contact_name"), number, json.optString("contact_description"), MmiOperationState.valueOf(json.optString("state", MmiOperationState.DIALING.name)), ForwardingVerification.valueOf(json.optString("verification", ForwardingVerification.NOT_CHECKED.name)), MmiEvidence(json.optBoolean("call_observed"), json.optLong("call_duration").takeIf { it > 0 }, json.optBoolean("watchdog_expired"), json.optString("ussd_response").takeIf { it.isNotBlank() }), json.optInt("target_subscription_id").takeIf { it >= 0 }, runCatching { DialPath.valueOf(json.optString("dial_path", DialPath.NOT_DISPATCHED.name)) }.getOrDefault(DialPath.NOT_DISPATCHED), json.optString("user_message").takeIf { it.isNotBlank() })
    }.getOrNull()

    fun clearPendingMmiRequest() = setPreference(KEY_PENDING_MMI_OPERATION, "")

    fun appendMmiAudit(entry: MmiAuditEntry) {
        val now = System.currentTimeMillis()
        val entries = runCatching {
            val raw = getPreference(KEY_MMI_AUDIT, "")
            if (raw.isBlank()) emptyList() else JSONArray(raw).let { array ->
                (0 until array.length()).mapNotNull { index -> runCatching {
                    val json = array.getJSONObject(index)
                    MmiAuditEntry(json.getLong("timestamp"), json.getString("id"), json.getString("action"), MmiExecutionMode.valueOf(json.getString("mode")), json.optInt("subscription_id").takeIf { it >= 0 }, DialPath.valueOf(json.optString("dial_path", DialPath.NOT_DISPATCHED.name)), ForwardingVerification.valueOf(json.getString("verification")), MmiEvidence(json.optBoolean("call_observed"), json.optLong("call_duration").takeIf { it > 0 }, json.optBoolean("watchdog_expired"), json.optString("ussd_response").takeIf { it.isNotBlank() }), json.optString("message").takeIf { it.isNotBlank() })
                }.getOrNull() }
            }
        }.getOrDefault(emptyList())
        val retained = MmiAuditRetentionPolicy.retain(entries + entry, now)
        val serialized = JSONArray().apply { retained.forEach { item -> put(JSONObject().apply {
            put("timestamp", item.timestampMillis); put("id", item.operationId); put("action", item.action); put("mode", item.executionMode.name); put("subscription_id", item.targetSubscriptionId); put("dial_path", item.dialPath.name); put("verification", item.verification.name); put("call_observed", item.evidence.callObserved); put("call_duration", item.evidence.callDurationMs); put("watchdog_expired", item.evidence.watchdogExpired); put("ussd_response", item.evidence.ussdResponse); put("message", item.message)
        }) } }
        setPreference(KEY_MMI_AUDIT, serialized.toString())
    }

    private fun pruneMmiAudit() {
        runCatching {
            val raw = getPreference(KEY_MMI_AUDIT, "")
            if (raw.isBlank()) return
            val now = System.currentTimeMillis()
            val retained = JSONArray().apply {
                val entries = JSONArray(raw)
                for (index in 0 until entries.length()) {
                    val entry = entries.optJSONObject(index) ?: continue
                    if (MmiAuditRetentionPolicy.shouldRetain(entry.optLong("timestamp"), now)) put(entry)
                }
            }
            setPreference(KEY_MMI_AUDIT, retained.toString())
        }.onFailure {
            setPreference(KEY_MMI_AUDIT, "")
            LoggingManager.logWarning(component = "SharedPreferencesManager", action = "MMI_AUDIT_INVALID", message = "Ungültiges MMI-Audit verworfen")
        }
    }

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
    fun setMmiActivatePrefix(prefix: String) = setMmiCodes(currentMmiCodes().copy(activatePrefix = sanitizeMmiInput(prefix)))

    fun getMmiActivatePrefix(): String =
        getPreference(KEY_MMI_ACTIVATE_PREFIX, DEFAULT_MMI_ACTIVATE_PREFIX)

    fun setMmiActivateSuffix(suffix: String) = setMmiCodes(currentMmiCodes().copy(activateSuffix = sanitizeMmiInput(suffix)))

    fun getMmiActivateSuffix(): String =
        getPreference(KEY_MMI_ACTIVATE_SUFFIX, DEFAULT_MMI_ACTIVATE_SUFFIX)

    fun setMmiDeactivateCode(code: String) = setMmiCodes(currentMmiCodes().copy(deactivateCode = sanitizeMmiInput(code)))

    fun getMmiDeactivateCode(): String =
        getPreference(KEY_MMI_DEACTIVATE_CODE, DEFAULT_MMI_DEACTIVATE_CODE)

    fun setMmiStatusCode(code: String) = setMmiCodes(currentMmiCodes().copy(statusCode = sanitizeMmiInput(code)))

    fun getMmiStatusCode(): String =
        getPreference(KEY_MMI_STATUS_CODE, DEFAULT_MMI_STATUS_CODE)

    fun resetMmiCodesToDefault() = applyMmiProfile(MmiCodeProfile.A1_SPECIAL)

    fun resetMmiCodesToGeneric() = applyMmiProfile(MmiCodeProfile.STANDARD_GSM)

    fun getMmiCodeProfile(): MmiCodeProfile = MmiCodeProfiles.detect(currentMmiCodes())

    fun isMmiConfigurationValid(): Boolean = currentMmiCodes().isValid()

    fun hasMmiProfileUserDecision(): Boolean = getPreference(KEY_MMI_PROFILE_USER_DECIDED, false)

    fun getA1HintShownScope(): String = getPreference(KEY_MMI_A1_HINT_SHOWN_SCOPE, "")
    fun setA1HintShownScope(scope: String) = setPreference(KEY_MMI_A1_HINT_SHOWN_SCOPE, scope)

    fun applyMmiProfile(profile: MmiCodeProfile) {
        require(profile != MmiCodeProfile.CUSTOM) { "CUSTOM hat keine vordefinierten Codes" }
        val codes = requireNotNull(MmiCodeProfiles.codesFor(profile))
        require(codes.isValid())
        setMmiCodes(codes)
        setPreference(KEY_MMI_PROFILE_USER_DECIDED, true)
    }

    private fun currentMmiCodes() = MmiCodeSet(
        getPreference(KEY_MMI_ACTIVATE_PREFIX, MmiCodeProfiles.standardGsm.activatePrefix),
        getPreference(KEY_MMI_ACTIVATE_SUFFIX, MmiCodeProfiles.standardGsm.activateSuffix),
        getPreference(KEY_MMI_DEACTIVATE_CODE, MmiCodeProfiles.standardGsm.deactivateCode),
        getPreference(KEY_MMI_STATUS_CODE, MmiCodeProfiles.standardGsm.statusCode),
    )

    private fun setMmiCodes(codes: MmiCodeSet) {
        prefs.edit()
            .putString(KEY_MMI_ACTIVATE_PREFIX, codes.activatePrefix)
            .putString(KEY_MMI_ACTIVATE_SUFFIX, codes.activateSuffix)
            .putString(KEY_MMI_DEACTIVATE_CODE, codes.deactivateCode)
            .putString(KEY_MMI_STATUS_CODE, codes.statusCode)
            .putString(KEY_MMI_CODE_PROFILE, MmiCodeProfiles.detect(codes).name)
            .apply()
    }

    private fun sanitizeMmiInput(value: String): String = value.filter { it in "*#0123456789" }

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
     * Merkt sich, dass der Nutzer den RCS-Hinweis auf der Startseite ausgeblendet hat.
     * @param dismissed true = Hinweis wird nicht mehr angezeigt
     */
    fun setRcsHintDismissed(dismissed: Boolean) =
        setPreference(KEY_RCS_HINT_DISMISSED, dismissed)

    /**
     * Prüft, ob der RCS-Hinweis bereits ausgeblendet wurde.
     * @return true wenn ausgeblendet, false wenn er angezeigt werden soll (Standard)
     */
    fun isRcsHintDismissed(): Boolean =
        getPreference(KEY_RCS_HINT_DISMISSED, false)

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

    // --- Betriebswarnungen ------------------------------------------------------------------
    // Zustaende, die die Weiterleitung beeintraechtigen, ohne sie zu stoppen. Sie liegen hier
    // und nicht in der Queue-Datei: Im Korruptionsfall ist gerade jene Datei die unlesbare.
    //
    // Diese Werte werden - anders als die uebrige Konfiguration - unter einer Sperre und mit
    // commit() geschrieben. Begruendung: Eine Einstellung kann der Nutzer jederzeit erneut
    // setzen; die Meldung ueber einen Verlust ist dagegen der einzige Beleg dafuer, dass etwas
    // verschwunden ist. Ginge sie bei einem Prozesskill verloren, waere der Verlust selbst
    // wieder kommentarlos - genau das, was Ziel 2 ausschliesst. Die Sperre macht ausserdem das
    // Read-modify-write der Zaehler atomar; zwei gleichzeitige Vorfaelle wuerden sonst denselben
    // Ausgangswert lesen und der Zaehler untererfasste den Schaden.

    private val warningLock = Any()

    /**
     * Haltbares, serialisiertes Read-modify-write eines Warnzustands.
     * @return `false`, wenn der Wert nicht synchron geschrieben werden konnte
     */
    private fun updateWarning(key: String, transform: (String) -> String): Boolean =
        synchronized(warningLock) {
            runCatching {
                val current = prefs.getString(key, "") ?: ""
                prefs.edit().putString(key, transform(current)).commit()
            }.getOrElse { error ->
                LoggingManager.logError(
                    component = "SharedPreferencesManager",
                    action = "WARNING_WRITE_FAILED",
                    message = "Warnzustand konnte nicht haltbar geschrieben werden: $key",
                    error = error
                )
                false
            }
        }

    /**
     * Vermerkt verlorene Queue-Eintraege. Mehrfache Vorfaelle werden aufsummiert, solange der
     * Nutzer sie nicht quittiert hat - sonst wuerde ein zweiter Vorfall den ersten verdecken.
     *
     * @param lostEntries Anzahl, oder [QueueCorruptionWarning.UNKNOWN_COUNT] bei unbekannter Menge
     * @return `false`, wenn die Meldung nicht haltbar geschrieben werden konnte
     */
    fun recordQueueCorruption(lostEntries: Int): Boolean =
        updateWarning(KEY_QUEUE_CORRUPTION) { current ->
            val existing = parseQueueCorruption(current)
            val total = when {
                existing == null -> lostEntries
                existing.lostEntries == QueueCorruptionWarning.UNKNOWN_COUNT ||
                    lostEntries == QueueCorruptionWarning.UNKNOWN_COUNT -> QueueCorruptionWarning.UNKNOWN_COUNT
                else -> existing.lostEntries + lostEntries
            }
            JSONObject().apply {
                put("timestamp", System.currentTimeMillis())
                put("lost_entries", total)
            }.toString()
        }

    fun getQueueCorruptionWarning(): QueueCorruptionWarning? =
        parseQueueCorruption(getPreference(KEY_QUEUE_CORRUPTION, ""))

    fun acknowledgeQueueCorruption(): Boolean = updateWarning(KEY_QUEUE_CORRUPTION) { "" }

    private fun parseQueueCorruption(raw: String): QueueCorruptionWarning? = runCatching {
        if (raw.isBlank()) return null
        val json = JSONObject(raw)
        QueueCorruptionWarning(json.getLong("timestamp"), json.getInt("lost_entries"))
    }.getOrNull()

    /**
     * Vermerkt eine Weiterleitung, die wegen voller Warteschlange nicht versucht wurde.
     *
     * Dies ist der **einzige** Beleg fuer diesen Verlust - in der Queue selbst kann er nicht
     * stehen, weil die Aufbewahrungsregel den Vermerk bei voller Queue sofort wieder verdraengen
     * wuerde. Entsprechend haltbar wird er geschrieben.
     *
     * @return `false`, wenn die Meldung nicht haltbar geschrieben werden konnte
     */
    fun recordDroppedForwarding(): Boolean =
        updateWarning(KEY_DROPPED_FORWARDINGS) { current ->
            JSONObject().apply {
                put("timestamp", System.currentTimeMillis())
                put("count", (parseDroppedForwarding(current)?.count ?: 0) + 1)
            }.toString()
        }

    fun getDroppedForwardingWarning(): DroppedForwardingWarning? =
        parseDroppedForwarding(getPreference(KEY_DROPPED_FORWARDINGS, ""))

    fun acknowledgeDroppedForwardings(): Boolean = updateWarning(KEY_DROPPED_FORWARDINGS) { "" }

    private fun parseDroppedForwarding(raw: String): DroppedForwardingWarning? = runCatching {
        if (raw.isBlank()) return null
        val json = JSONObject(raw)
        DroppedForwardingWarning(json.getLong("timestamp"), json.getInt("count"))
    }.getOrNull()

    /**
     * Merkt, dass die Statusanzeige mangels `POST_NOTIFICATIONS` unterdrueckt ist. Die
     * Weiterleitung laeuft dabei weiter - die Plattform verlangt die Berechtigung fuer einen
     * Foreground Service nicht.
     *
     * Kein Verlustbeleg, sondern eine Spiegelung des aktuellen Berechtigungsstands: Sie wird bei
     * jedem Dienststart und beim Oeffnen der App neu ermittelt und darf deshalb mit `apply()`
     * geschrieben werden.
     */
    fun setNotificationsSuppressed(suppressed: Boolean) =
        setPreference(KEY_NOTIFICATIONS_SUPPRESSED, suppressed)

    fun areNotificationsSuppressed(): Boolean = getPreference(KEY_NOTIFICATIONS_SUPPRESSED, false)

    /**
     * Vermerkt ein `Service.onTimeout()` des Foreground Service. Ein einmaliges Ereignis, das
     * sich nicht wiederholt - deshalb ebenfalls haltbar geschrieben.
     */
    fun recordServiceTimeout(timestampMillis: Long): Boolean =
        updateWarning(KEY_SERVICE_TIMEOUT_AT) { timestampMillis.toString() }

    fun getServiceTimeoutAt(): Long? =
        getPreference(KEY_SERVICE_TIMEOUT_AT, "").toLongOrNull()

    fun acknowledgeServiceTimeout(): Boolean = updateWarning(KEY_SERVICE_TIMEOUT_AT) { "" }

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
        private const val KEY_MMI_CODE_PROFILE = "mmi_code_profile"
        private const val KEY_MMI_PROFILE_MIGRATION_VERSION = "mmi_profile_migration_version"
        private const val KEY_MMI_PROFILE_USER_DECIDED = "mmi_profile_user_decided"
        private const val KEY_MMI_A1_HINT_SHOWN_SCOPE = "mmi_a1_hint_shown_scope"
        private const val KEY_SIM_SELECTION_MODE = "sim_selection_mode"
        private const val KEY_MMI_SIM_SELECTION_MODE = "mmi_sim_selection_mode"
        private const val KEY_INTERNATIONAL_DIAL_PREFIX = "international_dial_prefix"
        private const val KEY_FORWARDING_VERIFICATION = "forwarding_verification"
        private const val KEY_FORWARDING_CODE_SNAPSHOT = "forwarding_code_snapshot"
        private const val KEY_PENDING_MMI_OPERATION = "pending_mmi_operation"
        private const val KEY_MMI_AUDIT = "mmi_audit"
        private const val KEY_MAX_LOG_SIZE_MB = "max_log_size_mb"
        private const val KEY_PRIVACY_POLICY_ACCEPTED = "privacy_policy_accepted"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_SIM1_RECEIVE_ENABLED = "sim1_receive_enabled"
        private const val KEY_SIM2_RECEIVE_ENABLED = "sim2_receive_enabled"
        private const val KEY_RCS_HINT_DISMISSED = "rcs_hint_dismissed"
        private const val KEY_QUEUE_CORRUPTION = "forwarding_queue_corruption"
        private const val KEY_DROPPED_FORWARDINGS = "forwarding_dropped_queue_full"
        private const val KEY_NOTIFICATIONS_SUPPRESSED = "notifications_suppressed"
        private const val KEY_SERVICE_TIMEOUT_AT = "service_timeout_at"

        private const val MMI_PROFILE_MIGRATION_VERSION = 1
        private const val DEFAULT_MMI_ACTIVATE_PREFIX = "**21*"
        private const val DEFAULT_MMI_ACTIVATE_SUFFIX = "#"
        private const val DEFAULT_MMI_DEACTIVATE_CODE = "##21#"
        private const val DEFAULT_MMI_STATUS_CODE = "*#21#"

        // International Dial Prefix (Default für Österreich)
        private const val DEFAULT_INTERNATIONAL_DIAL_PREFIX = "00"

        // Log Settings (Default: 5MB)
        private const val DEFAULT_MAX_LOG_SIZE_MB = 5
    }
}

class PreferencesInitializationException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

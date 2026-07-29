package info.meuse24.smsforwarderneoA1.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import info.meuse24.smsforwarderneoA1.LoggingManager
import info.meuse24.smsforwarderneoA1.domain.model.EmailDeliveryState
import info.meuse24.smsforwarderneoA1.domain.model.EmailFailure
import info.meuse24.smsforwarderneoA1.domain.model.EmailFailureKind
import info.meuse24.smsforwarderneoA1.domain.model.EmailForwardingJob
import info.meuse24.smsforwarderneoA1.domain.model.EmailQueueRetentionPolicy
import info.meuse24.smsforwarderneoA1.domain.model.EmailRecipientState
import info.meuse24.smsforwarderneoA1.domain.model.QueueCorruptionWarning
import org.json.JSONArray
import org.json.JSONObject

/** Ausgang eines Zustandsuebergangs in der E-Mail-Queue. Bedeutung wie bei [QueueUpdate]. */
sealed interface EmailQueueUpdate {
    data class Applied(val job: EmailForwardingJob) : EmailQueueUpdate
    data class Rejected(val job: EmailForwardingJob) : EmailQueueUpdate
    data object NotStored : EmailQueueUpdate
}

/**
 * Persistente E-Mail-Queue in einer eigenen verschluesselten Datei.
 *
 * Aufbau und Begruendungen entsprechen [ForwardingQueueStore]:
 *
 * **Haltbarkeit:** Jeder Uebergang wird mit `commit()` geschrieben, nie mit `apply()`. `apply()`
 * schreibt asynchron und kann bei einem Force-Stop oder Low-Memory-Kill verloren gehen - also
 * genau in dem Szenario, fuer das diese Queue existiert.
 *
 * **Eigene Datei:** Konfiguration und Laufzeitdaten bleiben getrennt; eine defekte Queue reisst
 * die Konfiguration nicht mit. Der Korruptionswarnzustand liegt umgekehrt in den
 * Konfigurations-Preferences - im Ereignisfall ist ja gerade diese Datei die unlesbare.
 *
 * **Backup:** Die Datei ist in `backup_rules.xml` und `data_extraction_rules.xml` ausgeschlossen.
 * Sie enthaelt SMS-Volltexte; ohne den Ausschluss lieferte jedes Cloud-Backup und jede
 * Geraeteuebertragung sie mit aus.
 */
class EmailQueueStore(
    private val context: Context,
    private val prefsManager: SharedPreferencesManager
) {

    companion object {
        const val PREFS_NAME = "sms_forwarder_email_queue"
        const val SCHEMA_VERSION = 1
        private const val KEY_DOCUMENT = "queue_document"
        private const val FIELD_SCHEMA = "schema"
        private const val FIELD_ENTRIES = "entries"
    }

    /** Serialisiert das Read-modify-write. Macht nichts haltbar - das leistet nur `commit()`. */
    private val lock = Any()

    private val prefs: SharedPreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /** Alle Auftraege, aeltester zuerst. */
    fun all(): List<EmailForwardingJob> = synchronized(lock) { read() }

    /** Auftraege, auf die noch ein Automatismus wartet. */
    fun activeCount(): Int = all().count { !it.state.isTerminal }

    /** Auftraege, die eine Anzeige verdienen: gescheitert oder nur teilweise zugestellt. */
    fun unacknowledgedProblems(): List<EmailForwardingJob> = all().filter {
        !it.acknowledged &&
            (it.state == EmailDeliveryState.FAILED || it.state == EmailDeliveryState.PARTIAL)
    }

    /**
     * Legt einen Auftrag an. Gibt `false` zurueck, wenn das synchrone Schreiben fehlschlaegt -
     * dann darf nicht gesendet werden, weil der Auftrag sonst unauffindbar waere.
     */
    fun enqueue(job: EmailForwardingJob): Boolean = synchronized(lock) {
        val entries = read().filterNot { it.id == job.id } + job
        write(entries)
    }

    /** Wendet [transform] auf einen Auftrag an und schreibt das Ergebnis synchron. */
    fun update(
        id: String,
        transform: (EmailForwardingJob) -> EmailForwardingJob
    ): EmailQueueUpdate = synchronized(lock) {
        val entries = read()
        val existing = entries.firstOrNull { it.id == id } ?: return EmailQueueUpdate.NotStored
        val updated = transform(existing)
        if (updated == existing) return EmailQueueUpdate.Rejected(existing)
        return if (write(entries.map { if (it.id == id) updated else it })) {
            EmailQueueUpdate.Applied(updated)
        } else {
            EmailQueueUpdate.NotStored
        }
    }

    /** Wendet [transform] auf alle Auftraege an. Ein einziges `commit()` fuer den ganzen Durchlauf. */
    fun updateAll(transform: (EmailForwardingJob) -> EmailForwardingJob): List<EmailForwardingJob> =
        synchronized(lock) {
            val entries = read()
            val updated = entries.map(transform)
            if (updated == entries) return entries
            return if (write(updated)) updated else entries
        }

    /** Quittiert alle gescheiterten und teilweise zugestellten Auftraege. */
    fun acknowledgeProblems(): Boolean = synchronized(lock) {
        val entries = read()
        val updated = entries.map {
            if (!it.acknowledged &&
                (it.state == EmailDeliveryState.FAILED || it.state == EmailDeliveryState.PARTIAL)
            ) {
                it.copy(acknowledged = true)
            } else {
                it
            }
        }
        if (updated == entries) true else write(updated)
    }

    // --- Persistenz ---------------------------------------------------------------------

    private fun read(): List<EmailForwardingJob> {
        // Ob der Eintrag existiert, ist ohne Entschluesselung feststellbar - die Schluessel sind
        // deterministisch verschluesselt. Ohne diese Unterscheidung ginge ein Totalverlust
        // stillschweigend als leere Queue durch.
        val documentExists = runCatching { prefs.contains(KEY_DOCUMENT) }.getOrDefault(false)
        val raw = runCatching { prefs.getString(KEY_DOCUMENT, null) }.getOrNull()

        if (raw.isNullOrBlank()) {
            if (documentExists) {
                reportCorruption(QueueCorruptionWarning.UNKNOWN_COUNT, "document_undecryptable")
                runCatching { prefs.edit().remove(KEY_DOCUMENT).commit() }
            }
            return emptyList()
        }

        val document = runCatching { JSONObject(raw) }.getOrNull()
        if (document == null || document.optInt(FIELD_SCHEMA, 0) > SCHEMA_VERSION) {
            reportCorruption(QueueCorruptionWarning.UNKNOWN_COUNT, "document_unreadable")
            runCatching { prefs.edit().remove(KEY_DOCUMENT).commit() }
            return emptyList()
        }

        val array = document.optJSONArray(FIELD_ENTRIES) ?: JSONArray()
        var skipped = 0
        val entries = (0 until array.length()).mapNotNull { index ->
            val parsed = runCatching { deserialize(array.getJSONObject(index)) }.getOrNull()
            if (parsed == null) skipped++
            parsed
        }
        // Defekte Einzeleintraege werden uebersprungen, nicht die ganze Queue verworfen. Das
        // bereinigte Dokument wird sofort zurueckgeschrieben, damit derselbe Defekt nicht bei
        // jedem Lesen erneut als Verlust gemeldet wird.
        if (skipped > 0) {
            reportCorruption(skipped, "entries_unreadable")
            write(entries)
        }
        return entries
    }

    private fun write(entries: List<EmailForwardingJob>): Boolean {
        val retained = EmailQueueRetentionPolicy.retain(entries, System.currentTimeMillis())
        val document = JSONObject().apply {
            put(FIELD_SCHEMA, SCHEMA_VERSION)
            put(FIELD_ENTRIES, JSONArray().apply { retained.forEach { put(serialize(it)) } })
        }
        return runCatching {
            // commit() statt apply(): kehrt erst nach dem synchronen Schreiben zurueck.
            prefs.edit().putString(KEY_DOCUMENT, document.toString()).commit()
        }.getOrElse { error ->
            LoggingManager.logError(
                component = "EmailQueueStore",
                action = "WRITE_FAILED",
                message = "E-Mail-Queue konnte nicht geschrieben werden",
                error = error
            )
            false
        }
    }

    private fun serialize(job: EmailForwardingJob): JSONObject = JSONObject().apply {
        put("id", job.id)
        put("created_at", job.createdAtMillis)
        put("updated_at", job.updatedAtMillis)
        put("sender", job.sender)
        put("received_at", job.receivedAtMillis)
        put("body", job.body)
        put("state", job.state.name)
        put("attempt", job.attempt)
        put("next_attempt_at", job.nextAttemptAtMillis)
        put("acknowledged", job.acknowledged)
        job.lastFailure?.let { put("last_failure", serializeFailure(it)) }
        put("recipients", JSONArray().apply {
            job.recipients.forEach { recipient ->
                put(JSONObject().apply {
                    put("address", recipient.address)
                    put("delivered", recipient.delivered)
                    recipient.failure?.let { put("failure", serializeFailure(it)) }
                })
            }
        })
    }

    private fun serializeFailure(failure: EmailFailure): JSONObject = JSONObject().apply {
        put("kind", failure.kind.name)
        put("detail", failure.detail)
        put("return_code", failure.returnCode)
    }

    private fun deserialize(json: JSONObject): EmailForwardingJob = EmailForwardingJob(
        id = json.getString("id"),
        createdAtMillis = json.getLong("created_at"),
        updatedAtMillis = json.getLong("updated_at"),
        sender = json.optString("sender"),
        receivedAtMillis = json.optLong("received_at").takeIf { it > 0 }
            ?: json.getLong("created_at"),
        body = json.getString("body"),
        recipients = readRecipients(json),
        state = EmailDeliveryState.valueOf(json.getString("state")),
        attempt = json.optInt("attempt", 0),
        nextAttemptAtMillis = json.optLong("next_attempt_at").takeIf { it > 0 },
        lastFailure = json.optJSONObject("last_failure")?.let(::deserializeFailure),
        acknowledged = json.optBoolean("acknowledged", false)
    )

    private fun readRecipients(json: JSONObject): List<EmailRecipientState> {
        val array = json.optJSONArray("recipients") ?: JSONArray()
        return (0 until array.length()).map { index ->
            val entry = array.getJSONObject(index)
            EmailRecipientState(
                address = entry.getString("address"),
                delivered = entry.optBoolean("delivered", false),
                failure = entry.optJSONObject("failure")?.let(::deserializeFailure)
            )
        }
    }

    private fun deserializeFailure(json: JSONObject): EmailFailure? = runCatching {
        EmailFailure(
            kind = EmailFailureKind.valueOf(json.getString("kind")),
            detail = json.optString("detail").takeIf { it.isNotBlank() },
            returnCode = if (json.isNull("return_code")) null else json.optInt("return_code")
        )
    }.getOrNull()

    private fun reportCorruption(lostEntries: Int, cause: String) {
        val persisted = runCatching { prefsManager.recordQueueCorruption(lostEntries) }.getOrDefault(false)
        LoggingManager.logError(
            component = "EmailQueueStore",
            action = "QUEUE_CORRUPTION",
            message = "Eintraege der E-Mail-Queue verloren",
            details = mapOf(
                "cause" to cause,
                "lost_entries" to lostEntries,
                "warning_persisted" to persisted
            )
        )
    }
}

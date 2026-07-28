package info.meuse24.smsforwarderneoA1.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import info.meuse24.smsforwarderneoA1.LoggingManager
import info.meuse24.smsforwarderneoA1.domain.model.ForwardingOperation
import info.meuse24.smsforwarderneoA1.domain.model.ForwardingQueueRetentionPolicy
import info.meuse24.smsforwarderneoA1.domain.model.ForwardingState
import info.meuse24.smsforwarderneoA1.domain.model.QueueCorruptionWarning
import org.json.JSONArray
import org.json.JSONObject

/**
 * Ausgang eines Zustandsuebergangs in der Queue.
 *
 * Die drei Faelle sind bewusst unterscheidbar: Wer einen Vorgang in Besitz nehmen will, muss
 * [Applied] von [Rejected] trennen koennen. Ein blosses „nicht null" wuerde einen Vorgang, den
 * ein anderer Durchlauf gerade uebernommen hat, wie eine eigene Uebernahme aussehen lassen -
 * und damit doppelt senden.
 */
sealed interface QueueUpdate {
    /** Uebergang ausgefuehrt und haltbar geschrieben. */
    data class Applied(val operation: ForwardingOperation) : QueueUpdate

    /** Uebergang war nicht anwendbar; der Vorgang bleibt, wie er ist. */
    data class Rejected(val operation: ForwardingOperation) : QueueUpdate

    /** Eintrag nicht gefunden oder `commit()` fehlgeschlagen - der Seiteneffekt muss unterbleiben. */
    data object NotStored : QueueUpdate
}

/**
 * Persistente Weiterleitungs-Queue in einer eigenen verschluesselten Datei.
 *
 * **Haltbarkeit:** Jeder Zustandsuebergang wird mit `commit()` geschrieben, nie mit `apply()`.
 * `apply()` schreibt asynchron und kann bei `am kill` oder einem Low-Memory-Kill verloren gehen -
 * also genau in dem Szenario, fuer das diese Queue existiert. Schlaegt ein `commit()` fehl,
 * meldet die jeweilige Methode das an den Aufrufer, der den Seiteneffekt dann unterlaesst.
 *
 * **Eigene Datei:** Konfiguration und Laufzeitdaten sind getrennt, damit eine defekte Queue die
 * Konfiguration nicht mitreisst. Der Korruptionswarnzustand liegt umgekehrt in den
 * Konfigurations-Preferences - im Ereignisfall ist ja gerade diese Datei die unlesbare.
 *
 * **Kein Room:** Die Queue ist klein und ihr Inhalt maximal sensibel; `androidx.security.crypto`
 * ist bereits im Projekt und verschluesselt, eine Room-Datenbank waere es nicht.
 */
class ForwardingQueueStore(
    private val context: Context,
    private val prefsManager: SharedPreferencesManager
) {

    companion object {
        const val PREFS_NAME = "sms_forwarder_queue"
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

    /** Alle Vorgaenge, aeltester zuerst. */
    fun all(): List<ForwardingOperation> = synchronized(lock) { read() }

    /** Vorgaenge, die eine Anzeige verdienen: fehlgeschlagen oder ungeklaert, unquittiert. */
    fun unacknowledgedProblems(): List<ForwardingOperation> = all().filter {
        !it.acknowledged && (it.state == ForwardingState.FAILED || it.state == ForwardingState.UNKNOWN)
    }

    /**
     * Legt einen Vorgang an. Gibt `false` zurueck, wenn das synchrone Schreiben fehlschlaegt -
     * dann darf nicht gesendet werden, weil der Vorgang sonst unauffindbar waere.
     */
    fun enqueue(operation: ForwardingOperation): Boolean = synchronized(lock) {
        val entries = read().filterNot { it.id == operation.id } + operation
        write(entries)
    }

    /** Vorgaenge, auf die noch ein Automatismus wartet. */
    fun activeCount(): Int = all().count { !it.state.isTerminal }

    /**
     * Wendet [transform] auf einen Vorgang an und schreibt das Ergebnis synchron.
     *
     * Read-modify-write laeuft unter der Sperre, damit gleichzeitige Uebergaenge - etwa
     * Sendeversuch und eintreffender Callback - sich nicht gegenseitig ueberschreiben.
     */
    fun update(
        id: String,
        transform: (ForwardingOperation) -> ForwardingOperation
    ): QueueUpdate = synchronized(lock) {
        val entries = read()
        val existing = entries.firstOrNull { it.id == id } ?: return QueueUpdate.NotStored
        val updated = transform(existing)
        if (updated == existing) return QueueUpdate.Rejected(existing)
        return if (write(entries.map { if (it.id == id) updated else it })) {
            QueueUpdate.Applied(updated)
        } else {
            QueueUpdate.NotStored
        }
    }

    /** Wendet [transform] auf alle Vorgaenge an. Ein einziges `commit()` fuer den ganzen Durchlauf. */
    fun updateAll(transform: (ForwardingOperation) -> ForwardingOperation): List<ForwardingOperation> =
        synchronized(lock) {
            val entries = read()
            val updated = entries.map(transform)
            if (updated == entries) return entries
            return if (write(updated)) updated else entries
        }

    /** Quittiert alle fehlgeschlagenen und ungeklaerten Vorgaenge. */
    fun acknowledgeProblems(): Boolean = synchronized(lock) {
        val entries = read()
        val updated = entries.map {
            if (!it.acknowledged && (it.state == ForwardingState.FAILED || it.state == ForwardingState.UNKNOWN)) {
                it.copy(acknowledged = true)
            } else {
                it
            }
        }
        if (updated == entries) true else write(updated)
    }

    // --- Persistenz ---------------------------------------------------------------------

    private fun read(): List<ForwardingOperation> {
        val raw = runCatching { prefs.getString(KEY_DOCUMENT, null) }.getOrNull()
        if (raw.isNullOrBlank()) return emptyList()

        val document = runCatching { JSONObject(raw) }.getOrNull()
        if (document == null || document.optInt(FIELD_SCHEMA, 0) > SCHEMA_VERSION) {
            // Das Dokument als Ganzes ist unlesbar: neu anlegen und den Verlust sichtbar machen.
            reportCorruption(QueueCorruptionWarning.UNKNOWN_COUNT, "document_unreadable")
            prefs.edit().remove(KEY_DOCUMENT).commit()
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

    private fun write(entries: List<ForwardingOperation>): Boolean {
        val retained = ForwardingQueueRetentionPolicy.retain(entries, System.currentTimeMillis())
        val document = JSONObject().apply {
            put(FIELD_SCHEMA, SCHEMA_VERSION)
            put(FIELD_ENTRIES, JSONArray().apply { retained.forEach { put(serialize(it)) } })
        }
        return runCatching {
            // commit() statt apply(): kehrt erst nach dem synchronen Schreiben zurueck.
            prefs.edit().putString(KEY_DOCUMENT, document.toString()).commit()
        }.getOrElse { error ->
            LoggingManager.logError(
                component = "ForwardingQueueStore",
                action = "WRITE_FAILED",
                message = "Queue konnte nicht geschrieben werden",
                error = error
            )
            false
        }
    }

    private fun serialize(operation: ForwardingOperation): JSONObject = JSONObject().apply {
        put("id", operation.id)
        put("created_at", operation.createdAtMillis)
        put("updated_at", operation.updatedAtMillis)
        put("sender", operation.sender)
        put("target", operation.targetNumber)
        put("text", operation.text)
        put("subscription_id", operation.subscriptionId)
        put("expected_parts", operation.expectedParts)
        put("state", operation.state.name)
        put("attempt", operation.attempt)
        put("confirmed_part_indices", JSONArray(operation.confirmedPartIndices.sorted()))
        put("failed_part_indices", JSONArray(operation.failedPartIndices.sorted()))
        put("next_attempt_at", operation.nextAttemptAtMillis)
        put("handed_over_at", operation.handedOverAtMillis)
        put("last_result_code", operation.lastResultCode)
        put("reason", operation.reason)
        put("acknowledged", operation.acknowledged)
    }

    private fun deserialize(json: JSONObject): ForwardingOperation = ForwardingOperation(
        id = json.getString("id"),
        createdAtMillis = json.getLong("created_at"),
        updatedAtMillis = json.getLong("updated_at"),
        sender = json.optString("sender"),
        targetNumber = json.getString("target"),
        text = json.getString("text"),
        subscriptionId = json.optInt("subscription_id", -1),
        expectedParts = json.optInt("expected_parts", 1),
        state = ForwardingState.valueOf(json.getString("state")),
        attempt = json.optInt("attempt", 0),
        confirmedPartIndices = readIndices(json, "confirmed_part_indices"),
        failedPartIndices = readIndices(json, "failed_part_indices"),
        nextAttemptAtMillis = json.optLong("next_attempt_at").takeIf { it > 0 },
        handedOverAtMillis = json.optLong("handed_over_at").takeIf { it > 0 },
        lastResultCode = if (json.isNull("last_result_code")) null else json.optInt("last_result_code"),
        reason = json.optString("reason").takeIf { it.isNotBlank() },
        acknowledged = json.optBoolean("acknowledged", false)
    )

    private fun readIndices(json: JSONObject, field: String): Set<Int> {
        val array = json.optJSONArray(field) ?: return emptySet()
        return (0 until array.length()).map { array.getInt(it) }.toSet()
    }

    private fun reportCorruption(lostEntries: Int, cause: String) {
        // Der Warnzustand ist der einzige dauerhafte Beleg des Verlusts; misslingt sein
        // Schreiben, muss wenigstens das im Protokoll stehen.
        val persisted = runCatching { prefsManager.recordQueueCorruption(lostEntries) }.getOrDefault(false)
        LoggingManager.logError(
            component = "ForwardingQueueStore",
            action = "QUEUE_CORRUPTION",
            message = "Eintraege der Weiterleitungs-Queue verloren",
            details = mapOf(
                "cause" to cause,
                "lost_entries" to lostEntries,
                "warning_persisted" to persisted
            )
        )
    }
}

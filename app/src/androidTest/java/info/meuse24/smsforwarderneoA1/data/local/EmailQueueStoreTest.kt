package info.meuse24.smsforwarderneoA1.data.local

import androidx.test.platform.app.InstrumentationRegistry
import info.meuse24.smsforwarderneoA1.domain.model.EmailDeliveryReducer
import info.meuse24.smsforwarderneoA1.domain.model.EmailDeliveryState
import info.meuse24.smsforwarderneoA1.domain.model.EmailFailure
import info.meuse24.smsforwarderneoA1.domain.model.EmailFailureKind
import info.meuse24.smsforwarderneoA1.domain.model.EmailForwardingJob
import info.meuse24.smsforwarderneoA1.domain.model.EmailQueueRetentionPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Prueft die Haltbarkeit der E-Mail-Queue am Geraet - insbesondere, dass ein Auftrag eine
 * Neuerzeugung des Stores ueberlebt und dass ein unlesbares Dokument als **Verlust** gemeldet
 * wird statt als leere Queue durchzugehen.
 */
class EmailQueueStoreTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val now = System.currentTimeMillis()

    /** Schluessel des Queue-Dokuments. Das Format ist hier Pruefgegenstand, nicht Implementierungsdetail. */
    private val DOCUMENT_KEY = "queue_document"

    private fun newStore() = EmailQueueStore(context, SharedPreferencesManager(context))

    private fun job(id: String, vararg recipients: String): EmailForwardingJob =
        EmailDeliveryReducer.queue(
            id = id,
            now = now,
            sender = "+43664123456",
            receivedAtMillis = now - 5_000,
            body = "Hallo mit Umlauten: äöüß",
            recipients = recipients.toList()
        )

    @Before
    @After
    fun clearStores() {
        context.deleteSharedPreferences(EmailQueueStore.PREFS_NAME)
        context.deleteSharedPreferences("sms_forwarder_secure_prefs")
    }

    @Test
    fun aJobSurvivesRecreationOfTheStore() {
        assertTrue(newStore().enqueue(job("job-1", "a@example.org", "b@example.org")))

        val restored = newStore().all()

        assertEquals(1, restored.size)
        assertEquals("job-1", restored.first().id)
        assertEquals("Hallo mit Umlauten: äöüß", restored.first().body)
        assertEquals(now - 5_000, restored.first().receivedAtMillis)
        assertEquals(listOf("a@example.org", "b@example.org"), restored.first().pendingRecipients)
    }

    @Test
    fun aRecipientResultIsPersisted() {
        val store = newStore()
        store.enqueue(job("job-1", "a@example.org", "b@example.org"))
        store.update("job-1") { EmailDeliveryReducer.onAttemptStart(it, now) }

        store.update("job-1") { EmailDeliveryReducer.onRecipientDelivered(it, now, "a@example.org") }
        store.update("job-1") {
            EmailDeliveryReducer.onRecipientFailed(
                it,
                now,
                "b@example.org",
                EmailFailure(EmailFailureKind.RECIPIENT, "no such user", 550)
            )
        }

        val restored = newStore().all().first()
        assertTrue(restored.recipients.first { it.address == "a@example.org" }.delivered)
        assertEquals(
            EmailFailureKind.RECIPIENT,
            restored.recipients.first { it.address == "b@example.org" }.failure?.kind
        )
        assertEquals(550, restored.lastFailure?.returnCode)
    }

    /** Ohne die Inbesitznahme koennten zwei Durchlaeufe denselben Auftrag versenden. */
    @Test
    fun onlyOneRunCanTakeAJob() {
        val store = newStore()
        store.enqueue(job("job-1", "a@example.org"))

        val first = store.update("job-1") { EmailDeliveryReducer.onAttemptStart(it, now) }
        val second = store.update("job-1") { EmailDeliveryReducer.onAttemptStart(it, now) }

        assertTrue(first is EmailQueueUpdate.Applied)
        assertTrue(second is EmailQueueUpdate.Rejected)
    }

    @Test
    fun anUpdateOfAnUnknownJobIsNotStored() {
        assertTrue(newStore().update("missing") { it } is EmailQueueUpdate.NotStored)
    }

    @Test
    fun problemsAreReportedUntilAcknowledged() {
        val store = newStore()
        store.enqueue(job("job-1", "a@example.org"))
        store.update("job-1") { EmailDeliveryReducer.onAttemptStart(it, now) }
        store.update("job-1") {
            EmailDeliveryReducer.onRecipientFailed(
                it,
                now,
                "a@example.org",
                EmailFailure(EmailFailureKind.AUTHENTICATION, "535")
            )
        }
        store.update("job-1") { EmailDeliveryReducer.onAttemptFinished(it, now) }

        assertEquals(EmailDeliveryState.FAILED, store.all().first().state)
        assertEquals(1, store.unacknowledgedProblems().size)

        assertTrue(store.acknowledgeProblems())
        assertEquals(0, newStore().unacknowledgedProblems().size)
    }

    @Test
    fun activeCountIgnoresTerminalJobs() {
        val store = newStore()
        store.enqueue(job("running", "a@example.org"))
        store.enqueue(job("done", "b@example.org"))
        store.update("done") { EmailDeliveryReducer.onAttemptStart(it, now) }
        store.update("done") { EmailDeliveryReducer.onRecipientDelivered(it, now, "b@example.org") }
        store.update("done") { EmailDeliveryReducer.onAttemptFinished(it, now) }

        assertEquals(1, store.activeCount())
    }

    /**
     * Der wichtigste Fall: Ein vorhandenes, aber nicht entschluesselbares Dokument darf nicht als
     * leere Queue durchgehen - der Verlust muss ausserhalb der Queue vermerkt werden.
     */
    @Test
    fun anUndecryptableDocumentIsReportedAsLoss() {
        newStore().enqueue(job("job-1", "a@example.org"))

        // Den verschluesselten Wert durch Unsinn ersetzen. Der Schluessel bleibt auffindbar, der
        // Wert ist nicht mehr zu entschluesseln - genau der Fall, der ohne die contains-Pruefung
        // als leere Queue durchginge. Die Keyset-Eintraege von Tink bleiben unangetastet.
        context.getSharedPreferences(EmailQueueStore.PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .let { raw ->
                val key = raw.all.keys.first { !it.startsWith("__androidx_security") }
                raw.edit().putString(key, "not-a-valid-ciphertext").commit()
            }

        val prefs = SharedPreferencesManager(context)
        prefs.acknowledgeQueueCorruption()

        val entries = EmailQueueStore(context, prefs).all()

        assertEquals(0, entries.size)
        assertNotNull("Der Verlust muss dauerhaft vermerkt sein", prefs.getQueueCorruptionWarning())
    }

    /** Ein defekter Einzeleintrag darf nicht die ganze Queue mitreissen. */
    @Test
    fun aBrokenSingleEntryDoesNotDiscardTheRest() {
        val store = newStore()
        store.enqueue(job("job-1", "a@example.org"))
        store.enqueue(job("job-2", "b@example.org"))

        // Einem Eintrag ein Pflichtfeld nehmen. Das Dokument bleibt gueltiges JSON, nur dieser
        // eine Eintrag ist nicht mehr lesbar.
        val encrypted = openEncrypted()
        val document = org.json.JSONObject(encrypted.getString(DOCUMENT_KEY, null)!!)
        document.getJSONArray("entries").getJSONObject(0).remove("state")
        encrypted.edit().putString(DOCUMENT_KEY, document.toString()).commit()

        val prefs = SharedPreferencesManager(context)
        prefs.acknowledgeQueueCorruption()
        val entries = EmailQueueStore(context, prefs).all()

        assertEquals(1, entries.size)
        assertEquals("job-2", entries.first().id)
        assertEquals(1, prefs.getQueueCorruptionWarning()?.lostEntries)
        // Das bereinigte Dokument wurde zurueckgeschrieben - derselbe Defekt wird nicht bei jedem
        // Lesen erneut als Verlust gemeldet.
        assertEquals(1, newStore().all().size)
    }

    private fun openEncrypted() = androidx.security.crypto.EncryptedSharedPreferences.create(
        context,
        EmailQueueStore.PREFS_NAME,
        androidx.security.crypto.MasterKey.Builder(context)
            .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
            .build(),
        androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    @Test
    fun theRetentionLimitIsEnforcedOnWrite() {
        val store = newStore()
        repeat(EmailQueueRetentionPolicy.MAX_ENTRIES + 3) { index ->
            val terminal = job("job-$index", "a@example.org")
                .copy(state = EmailDeliveryState.SENT, createdAtMillis = now + index)
            store.enqueue(terminal)
        }

        assertEquals(EmailQueueRetentionPolicy.MAX_ENTRIES, store.all().size)
    }
}

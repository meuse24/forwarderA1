package info.meuse24.smsforwarderneoA1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MmiAuditRetentionPolicyTest {
    private val now = 2_000_000_000_000L

    @Test
    fun `retains the thirty day boundary and removes an older entry`() {
        assertTrue(MmiAuditRetentionPolicy.shouldRetain(now - MmiAuditRetentionPolicy.RETENTION_MS, now))
        assertFalse(MmiAuditRetentionPolicy.shouldRetain(now - MmiAuditRetentionPolicy.RETENTION_MS - 1, now))

        val retained = MmiAuditRetentionPolicy.retain(
            listOf(entry("expired", now - MmiAuditRetentionPolicy.RETENTION_MS - 1), entry("boundary", now - MmiAuditRetentionPolicy.RETENTION_MS)),
            now
        )

        assertEquals(listOf("boundary"), retained.map { it.operationId })
    }

    @Test
    fun `keeps only the newest two hundred retained audit entries`() {
        val retained = MmiAuditRetentionPolicy.retain(
            (200 downTo 0).map { entry("id-$it", now - it) },
            now
        )

        assertEquals(200, retained.size)
        assertEquals("id-199", retained.first().operationId)
        assertEquals("id-0", retained.last().operationId)
    }

    private fun entry(id: String, timestamp: Long) = MmiAuditEntry(
        timestampMillis = timestamp,
        operationId = id,
        action = "ACTIVATE",
        executionMode = MmiExecutionMode.VOICE_MMI_CALL,
        targetSubscriptionId = null,
        dialPath = DialPath.TELECOM_MANAGER,
        verification = ForwardingVerification.ASSUMED_SUCCESS,
        evidence = MmiEvidence(),
        message = null
    )
}

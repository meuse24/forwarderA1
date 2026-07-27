package info.meuse24.smsforwarderneoA1.domain.model

/** Android-free retention rules for the local, sanitised MMI audit ring buffer. */
object MmiAuditRetentionPolicy {
    const val RETENTION_MS: Long = 30L * 24 * 60 * 60 * 1000
    const val MAX_ENTRIES: Int = 200

    fun shouldRetain(timestampMillis: Long, nowMillis: Long): Boolean =
        timestampMillis >= nowMillis - RETENTION_MS

    fun retain(entries: List<MmiAuditEntry>, nowMillis: Long): List<MmiAuditEntry> =
        entries.filter { shouldRetain(it.timestampMillis, nowMillis) }.takeLast(MAX_ENTRIES)
}

package info.meuse24.smsforwarderneoA1.domain.model

/** Sanitised local diagnostic record. It deliberately contains no phone number, code or raw carrier response. */
data class MmiAuditEntry(
    val timestampMillis: Long,
    val operationId: String,
    val action: String,
    val executionMode: MmiExecutionMode,
    val targetSubscriptionId: Int?,
    val dialPath: DialPath,
    val verification: ForwardingVerification,
    val evidence: MmiEvidence,
    val message: String?
)

package info.meuse24.smsforwarderneoA1.domain.model

/** Code path that must be retained while a network forwarding is active. */
data class ForwardingCodeSnapshot(
    val deactivateCode: String,
    val executionMode: MmiExecutionMode,
    val subscriptionId: Int?,
)

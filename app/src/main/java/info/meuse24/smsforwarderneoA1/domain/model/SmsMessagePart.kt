package info.meuse24.smsforwarderneoA1.domain.model

/**
 * Ein einzelner Teil einer empfangenen SMS, losgeloest von der Plattform.
 *
 * Liegt im Domain-Paket, damit Gruppierung und Absenderermittlung ohne Geraet
 * pruefbar sind (siehe [SmsForwardingComposer]).
 */
data class SmsMessagePart(
    val body: String,
    val timestamp: Long,
    val referenceNumber: Int,      // Hash der intentId - gruppiert Parts desselben Intents
    val sequencePosition: Int,     // Array-Index aus getMessagesFromIntent() = Reihenfolge der Parts
    val totalParts: Int,
    val sender: String,
    val subscriptionId: Int = -1,  // -1 = unbekannt/nicht verfuegbar
    val intentId: String           // Unique UUID pro empfangenem Intent
)

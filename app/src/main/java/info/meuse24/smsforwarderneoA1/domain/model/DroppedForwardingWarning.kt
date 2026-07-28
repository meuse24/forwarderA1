package info.meuse24.smsforwarderneoA1.domain.model

/**
 * Persistenter Hinweis auf Weiterleitungen, die gar nicht erst versucht wurden, weil die
 * Warteschlange voll war.
 *
 * Liegt bewusst **ausserhalb** der Queue: Ein Vermerk *in* der Queue waere genau der Eintrag, den
 * die Aufbewahrungsregel im Ereignisfall als naechstes verdraengt - der Hinweis wuerde sich selbst
 * loeschen. Ein Snackbar allein genuegt ebenfalls nicht; er ist weg, bevor jemand hinsieht.
 */
data class DroppedForwardingWarning(
    val timestampMillis: Long,
    val count: Int
)

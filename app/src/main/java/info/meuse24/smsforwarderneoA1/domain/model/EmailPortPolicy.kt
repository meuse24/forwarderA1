package info.meuse24.smsforwarderneoA1.domain.model

/**
 * Pruefung der Porteingabe.
 *
 * Frueher wurde eine unbrauchbare Eingabe stillschweigend auf den alten Wert zurueckgesetzt: Der
 * Nutzer sah seine Eingabe verschwinden, ohne zu erfahren warum, und ein Wert wie `0` oder
 * `70000` wurde widerspruchslos gespeichert.
 */
object EmailPortPolicy {

    const val MIN_PORT = 1
    const val MAX_PORT = 65535

    sealed interface Result {
        data class Valid(val port: Int) : Result
        data object Empty : Result
        data object NotANumber : Result
        data object OutOfRange : Result
    }

    fun validate(input: String): Result {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return Result.Empty
        val port = trimmed.toIntOrNull() ?: return Result.NotANumber
        if (port < MIN_PORT || port > MAX_PORT) return Result.OutOfRange
        return Result.Valid(port)
    }
}

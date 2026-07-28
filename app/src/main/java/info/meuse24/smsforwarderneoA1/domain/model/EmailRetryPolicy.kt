package info.meuse24.smsforwarderneoA1.domain.model

import java.io.IOException

/**
 * Wiederholungsregel des E-Mail-Kanals.
 *
 * Der Kanal ist vom SMS-Kanal getrennt: Ein SMTP-Ausfall darf keine zusaetzliche,
 * kostenpflichtige SMS ausloesen. Wiederholt wird deshalb ausschliesslich der E-Mail-Versand.
 */
object EmailRetryPolicy {

    const val MAX_RETRIES = 3
    private const val INITIAL_DELAY_MILLIS = 5_000L

    /**
     * Wiederholt wird nur bei Transportfehlern. `javax.mail.MessagingException` wird ueber den
     * Klassennamen erkannt, damit diese Regel ohne Mail-Abhaengigkeit pruefbar bleibt.
     */
    fun isRetryable(error: Throwable): Boolean =
        error is IOException || generationOf(error).any { it.simpleName == "MessagingException" }

    /** Lineares Backoff: 5 s, 10 s, 15 s. @param attempt beginnt bei 1. */
    fun delayMillis(attempt: Int): Long = INITIAL_DELAY_MILLIS * attempt.coerceAtLeast(1)

    /** Weiterer Versuch erlaubt? @param attempt Anzahl bereits unternommener Versuche. */
    fun shouldRetry(error: Throwable, attempt: Int): Boolean =
        attempt <= MAX_RETRIES && isRetryable(error)

    private fun generationOf(error: Throwable): Sequence<Class<*>> =
        generateSequence(error.javaClass as Class<*>) { it.superclass }
}

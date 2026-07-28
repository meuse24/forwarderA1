package info.meuse24.smsforwarderneoA1.domain.model

/**
 * Zustandsmaschine der SMS-Weiterleitung, frei von Android.
 *
 * Leitregel: Neu gesendet wird nur dort, wo ein Fehlschlag belegt ist. Ein negativer
 * Callback ist eine positive Aussage des Frameworks ueber *diesen* Teil und rechtfertigt
 * den Neuversand. Das Fehlen jeder Aussage rechtfertigt ihn nicht - solche Vorgaenge
 * enden in [ForwardingState.UNKNOWN] und werden nur angezeigt.
 *
 * Bewusst in Kauf genommen: Bei mehrteiligen Nachrichten mit Teilerfolg wird der gesamte
 * Vorgang neu versandt; bereits bestaetigte Teile koennen dadurch doppelt ankommen.
 * Einzelne Segmente lassen sich ueber sendMultipartTextMessage nicht gezielt nachsenden,
 * und ein unvollstaendiges Fragment wird vom Empfaengergeraet nie zusammengesetzt.
 */
object SmsDeliveryReducer {

    /** Neuversuche nach dem ersten Sendeversuch. */
    const val MAX_RETRIES = 3

    /** Erster Versuch plus [MAX_RETRIES] Wiederholungen. */
    const val MAX_ATTEMPTS = MAX_RETRIES + 1

    /** Backoff vor dem 1./2./3. Neuversuch. */
    val RETRY_BACKOFF_MILLIS = longArrayOf(30_000L, 120_000L, 600_000L)

    /** Frist, nach der ein uebergebener Vorgang ohne vollstaendige Rueckmeldung mehrdeutig ist. */
    const val HANDOVER_TIMEOUT_MILLIS = 15 * 60 * 1000L

    /**
     * Exception-Typen, die eine verletzte Vorbedingung belegen und daher [ForwardingState.FAILED]
     * ergeben duerfen. Als benannte Liste, damit die Entscheidung an einer Stelle steht und nicht
     * ueber catch-Zweige verstreut ist.
     *
     * Alles andere - insbesondere alles aus dem Binder-Aufruf selbst (DeadObjectException,
     * RemoteException) - beweist nicht, dass keine Uebergabe stattgefunden hat, und ergibt
     * [ForwardingState.UNKNOWN].
     */
    val PRECONDITION_FAILURE_TYPES: List<Class<out Throwable>> = listOf(
        IllegalArgumentException::class.java,
        SecurityException::class.java,
        NullPointerException::class.java
    )

    /** Vorgang anlegen: Zieldaten und erwartete Teilzahl stehen fest, gesendet wurde nichts. */
    fun queue(
        id: String,
        now: Long,
        sender: String,
        targetNumber: String,
        text: String,
        subscriptionId: Int,
        expectedParts: Int
    ): ForwardingOperation = ForwardingOperation(
        id = id,
        createdAtMillis = now,
        updatedAtMillis = now,
        sender = sender,
        targetNumber = targetNumber,
        text = text,
        subscriptionId = subscriptionId,
        expectedParts = expectedParts.coerceAtLeast(1),
        state = ForwardingState.QUEUED
    )

    /**
     * Uebergang unmittelbar vor dem Sendeaufruf - und zugleich die **Inbesitznahme** des
     * Vorgangs.
     *
     * Der Uebergang ist nur aus einem sendefaelligen Zustand erlaubt ([isDispatchDue]). Aus jedem
     * anderen Zustand bleibt der Vorgang unveraendert; der Aufrufer erkennt daran, dass ein
     * anderer Durchlauf schneller war, und darf **nicht** senden. Ohne diese Bedingung koennten
     * der unmittelbare Versand nach dem Einreihen und ein gleichzeitiger Queue-Scan denselben
     * Vorgang zweimal hinausschicken.
     *
     * Die Rueckmeldungen des vorigen Versuchs werden verworfen; Callbacks des alten Versuchs sind
     * ueber die Versuchsnummer erkennbar.
     */
    fun onAttemptStart(operation: ForwardingOperation, now: Long, expectedParts: Int): ForwardingOperation =
        if (!isDispatchDue(operation, now)) {
            operation
        } else {
            operation.copy(
                state = ForwardingState.ATTEMPTING,
                attempt = operation.attempt + 1,
                expectedParts = expectedParts.coerceAtLeast(1),
                confirmedPartIndices = emptySet(),
                failedPartIndices = emptySet(),
                nextAttemptAtMillis = null,
                handedOverAtMillis = null,
                lastResultCode = null,
                reason = null,
                updatedAtMillis = now
            )
        }

    /**
     * Der Sendeaufruf ist ohne Exception zurueckgekehrt.
     *
     * Wirkt nur auf [ForwardingState.ATTEMPTING]: Ein Callback kann schneller sein als das
     * Schreiben dieses Zustands und darf nicht zurueckgeworfen werden.
     */
    fun onHandedOver(operation: ForwardingOperation, now: Long): ForwardingOperation =
        if (operation.state == ForwardingState.ATTEMPTING) {
            operation.copy(
                state = ForwardingState.HANDED_OVER,
                handedOverAtMillis = now,
                updatedAtMillis = now
            )
        } else {
            operation
        }

    /**
     * Vorbedingung verletzt, bevor [ForwardingState.ATTEMPTING] geschrieben wurde.
     *
     * Gilt - wie die Inbesitznahme - nur aus einem sendefaelligen Zustand. Andernfalls hat ein
     * anderer Durchlauf den Vorgang laengst uebernommen; ihn dann auf `FAILED` zu setzen, wuerde
     * einen laufenden Versand fuer erledigt erklaeren und dessen Callbacks wirkungslos machen.
     * Die Vorbedingungspruefung eines veralteten Durchlaufs sagt ueber den laufenden nichts aus.
     */
    fun onPreconditionRejected(operation: ForwardingOperation, now: Long, reason: String): ForwardingOperation =
        if (!isDispatchDue(operation, now)) {
            operation
        } else {
            operation.copy(
                state = ForwardingState.FAILED,
                reason = reason,
                updatedAtMillis = now
            )
        }

    /**
     * Exception aus dem Sendeaufruf, also nach [ForwardingState.ATTEMPTING].
     *
     * Nur ein Typ aus [PRECONDITION_FAILURE_TYPES] belegt, dass keine Uebergabe stattfand.
     * Jede andere Exception ergibt [ForwardingState.UNKNOWN] - sie als Fehlschlag darzustellen
     * waere eine Aussage, die die Plattform nicht hergibt.
     */
    fun onDispatchException(operation: ForwardingOperation, now: Long, error: Throwable): ForwardingOperation {
        val precondition = PRECONDITION_FAILURE_TYPES.any { it.isInstance(error) }
        return operation.copy(
            state = if (precondition) ForwardingState.FAILED else ForwardingState.UNKNOWN,
            reason = "${error.javaClass.simpleName}: ${error.message ?: ""}".trim(),
            updatedAtMillis = now
        )
    }

    /**
     * Rueckmeldung eines Teils. [attempt] und [partIndex] stammen aus der Callback-URI.
     *
     * Folgenlos bleiben: Rueckmeldungen eines frueheren Versuchs (ihre Zaehlung gilt nicht mehr),
     * ein bereits gemeldeter Teil (doppelte Zustellung desselben Callbacks) und ein Teilindex
     * ausserhalb der erwarteten Teilzahl. Alle drei sind Aussagen, die nichts Neues belegen -
     * und ohne diese Pruefung wuerde eine doppelte Bestaetigung einen Vorgang als vollstaendig
     * ausweisen, dessen anderer Teil nie ankam.
     */
    fun onPartResult(
        operation: ForwardingOperation,
        attempt: Int,
        partIndex: Int,
        result: SmsPartResult,
        resultCode: Int,
        now: Long
    ): ForwardingOperation {
        if (attempt != operation.attempt) return operation
        if (operation.state != ForwardingState.ATTEMPTING && operation.state != ForwardingState.HANDED_OVER) {
            return operation
        }
        if (partIndex < 0 || partIndex >= operation.expectedParts) return operation
        if (operation.hasReported(partIndex)) return operation

        val updated = operation.copy(
            confirmedPartIndices = if (result == SmsPartResult.OK) {
                operation.confirmedPartIndices + partIndex
            } else {
                operation.confirmedPartIndices
            },
            failedPartIndices = if (result == SmsPartResult.OK) {
                operation.failedPartIndices
            } else {
                operation.failedPartIndices + partIndex
            },
            lastResultCode = resultCode,
            updatedAtMillis = now
        )

        return when {
            // Ein terminaler Fehler heilt durch keinen Neuversand.
            result == SmsPartResult.TERMINAL_FAILURE -> updated.copy(
                state = ForwardingState.FAILED,
                reason = "result_code=$resultCode"
            )

            result == SmsPartResult.TRANSIENT_FAILURE -> scheduleRetryOrFail(updated, now, "result_code=$resultCode")

            // SENT erst bei Vollzaehligkeit - eine unvollstaendige Multipart-SMS ist wertlos.
            updated.confirmedParts >= updated.expectedParts -> updated.copy(state = ForwardingState.SENT)

            else -> updated
        }
    }

    private fun scheduleRetryOrFail(
        operation: ForwardingOperation,
        now: Long,
        reason: String
    ): ForwardingOperation {
        if (operation.attempt >= MAX_ATTEMPTS) {
            return operation.copy(
                state = ForwardingState.FAILED,
                reason = "$reason, retries_exhausted",
                nextAttemptAtMillis = null
            )
        }
        val backoffIndex = (operation.attempt - 1).coerceIn(0, RETRY_BACKOFF_MILLIS.lastIndex)
        return operation.copy(
            state = ForwardingState.RETRY,
            reason = reason,
            nextAttemptAtMillis = now + RETRY_BACKOFF_MILLIS[backoffIndex]
        )
    }

    /**
     * Erster Durchlauf nach einem Prozessstart: Ein vorgefundenes [ForwardingState.ATTEMPTING]
     * ist mehrdeutig, weil zwischen dem Schreiben und dem Sendeaufruf ein Prozessverlust
     * moeglich war. Kein Neuversand.
     */
    fun onProcessRestart(operation: ForwardingOperation, now: Long): ForwardingOperation =
        if (operation.state == ForwardingState.ATTEMPTING) {
            operation.copy(
                state = ForwardingState.UNKNOWN,
                reason = "attempting_found_after_restart",
                updatedAtMillis = now
            )
        } else {
            operation
        }

    /** Ablauf-Scan: uebergeben, aber seit 15 Minuten ohne vollstaendige Rueckmeldung. */
    fun onExpiryScan(operation: ForwardingOperation, now: Long): ForwardingOperation {
        if (operation.state != ForwardingState.HANDED_OVER) return operation
        val handedOver = operation.handedOverAtMillis ?: operation.updatedAtMillis
        if (now - handedOver < HANDOVER_TIMEOUT_MILLIS) return operation
        return operation.copy(
            state = ForwardingState.UNKNOWN,
            reason = "no_callback_within_${HANDOVER_TIMEOUT_MILLIS}ms",
            updatedAtMillis = now
        )
    }

    /** Ein Vorgang, dessen Sendeaufruf ansteht: frisch eingereiht oder faelliger Neuversuch. */
    fun isDispatchDue(operation: ForwardingOperation, now: Long): Boolean = when (operation.state) {
        ForwardingState.QUEUED -> true
        ForwardingState.RETRY -> (operation.nextAttemptAtMillis ?: 0L) <= now
        else -> false
    }
}

package info.meuse24.smsforwarderneoA1.domain.model

/**
 * Zustandsmaschine des E-Mail-Kanals, frei von Android.
 *
 * Leitregel: **Im Zweifel erneut senden.** Das ist die bewusste Umkehrung der SMS-Regel
 * ([SmsDeliveryReducer]) und gilt nur hier: Eine E-Mail kostet nichts, der Verlust einer
 * weitergeleiteten SMS ist dagegen der eigentliche Schadensfall. Die Dublette wird zusaetzlich
 * durch eine stabile Message-ID abgeschwaecht, mit der viele Server und Clients die Wiederholung
 * erkennen.
 *
 * Jede Funktion gibt bei nicht anwendbarem Uebergang das **unveraenderte** Objekt zurueck. Der
 * Store erkennt daran, dass nichts zu schreiben ist, und der Aufrufer, dass ein anderer Durchlauf
 * schneller war.
 */
object EmailDeliveryReducer {

    /** Auftrag anlegen. Empfaengerliste steht fest, gesendet wurde nichts. */
    fun queue(
        id: String,
        now: Long,
        sender: String,
        receivedAtMillis: Long,
        body: String,
        recipients: List<String>
    ): EmailForwardingJob = EmailForwardingJob(
        id = id,
        createdAtMillis = now,
        updatedAtMillis = now,
        sender = sender,
        receivedAtMillis = receivedAtMillis,
        body = body,
        recipients = recipients.distinct().map { EmailRecipientState(it) },
        state = EmailDeliveryState.QUEUED
    )

    /**
     * Uebergang unmittelbar vor dem Verbindungsaufbau - und zugleich die **Inbesitznahme**.
     *
     * Nur aus einem versandfaelligen Zustand erlaubt. Aus jedem anderen bleibt der Auftrag
     * unveraendert; der Aufrufer erkennt daran, dass ein anderer Durchlauf ihn bereits sendet,
     * und darf nicht senden. Ohne diese Bedingung koennten der Anstoss nach dem Einreihen und ein
     * gleichzeitiger Queue-Scan denselben Auftrag zweimal versenden.
     */
    fun onAttemptStart(job: EmailForwardingJob, now: Long): EmailForwardingJob =
        if (!isDispatchDue(job, now)) {
            job
        } else {
            job.copy(
                state = EmailDeliveryState.ATTEMPTING,
                attempt = job.attempt + 1,
                nextAttemptAtMillis = null,
                updatedAtMillis = now
            )
        }

    /** Ein Empfaenger wurde zugestellt. Ein etwaiger frueherer Fehler dieses Empfaengers entfaellt. */
    fun onRecipientDelivered(job: EmailForwardingJob, now: Long, address: String): EmailForwardingJob =
        updateRecipient(job, now, address) { it.copy(delivered = true, failure = null) }

    /** Ein Empfaenger wurde abgelehnt. Ueber andere Empfaenger sagt das nichts aus. */
    fun onRecipientFailed(
        job: EmailForwardingJob,
        now: Long,
        address: String,
        failure: EmailFailure
    ): EmailForwardingJob =
        updateRecipient(job, now, address) { it.copy(failure = failure) }
            .let { if (it === job) it else it.copy(lastFailure = failure) }

    /**
     * Der Verbindungsaufbau ist gescheitert - kein Empfaenger wurde beliefert.
     *
     * Der Fehler gilt fuer alle noch offenen Empfaenger gleichermassen. Ein Anmeldefehler etwa
     * betrifft nicht eine Adresse, sondern den ganzen Auftrag.
     */
    fun onConnectionFailed(job: EmailForwardingJob, now: Long, failure: EmailFailure): EmailForwardingJob {
        if (job.state != EmailDeliveryState.ATTEMPTING) return job
        return job.copy(
            recipients = job.recipients.map { if (it.isPending) it.copy(failure = failure) else it },
            lastFailure = failure,
            updatedAtMillis = now
        )
    }

    /**
     * Abschluss eines Versuchs: aus den Empfaengerergebnissen wird der Auftragszustand.
     *
     * Der Fristablauf wird hier mitgeprueft, damit ein Auftrag nicht erst beim naechsten Scan
     * terminal wird.
     */
    fun onAttemptFinished(job: EmailForwardingJob, now: Long): EmailForwardingJob {
        if (job.state != EmailDeliveryState.ATTEMPTING) return job

        val pending = job.recipients.count { it.isPending }
        val delivered = job.deliveredCount

        val state = when {
            pending == 0 && delivered == job.recipients.size -> EmailDeliveryState.SENT
            pending == 0 && delivered > 0 -> EmailDeliveryState.PARTIAL
            pending == 0 -> EmailDeliveryState.FAILED
            EmailRetryPolicy.isExpired(job.createdAtMillis, now) ->
                if (delivered > 0) EmailDeliveryState.PARTIAL else EmailDeliveryState.FAILED

            else -> EmailDeliveryState.RETRY
        }

        return job.copy(
            state = state,
            nextAttemptAtMillis = if (state == EmailDeliveryState.RETRY) {
                now + EmailRetryPolicy.delayMillis(job.attempt)
            } else {
                null
            },
            updatedAtMillis = now
        )
    }

    /**
     * Erster Durchlauf nach einem Prozessstart.
     *
     * Ein vorgefundenes [EmailDeliveryState.ATTEMPTING] ist mehrdeutig: Der Server kann die
     * Nachricht angenommen haben, bevor der Prozess starb. Anders als beim SMS-Kanal wird hier
     * **erneut gesendet** - siehe Leitregel. Zugestellte Empfaenger sind dabei bereits vermerkt
     * und werden nicht noch einmal angeschrieben; das Fenster bleibt auf den einen Empfaenger
     * beschraenkt, dessen Ergebnis nicht mehr geschrieben werden konnte.
     */
    fun onProcessRestart(job: EmailForwardingJob, now: Long): EmailForwardingJob =
        if (job.state == EmailDeliveryState.ATTEMPTING) {
            job.copy(
                state = EmailDeliveryState.QUEUED,
                nextAttemptAtMillis = null,
                updatedAtMillis = now
            )
        } else {
            job
        }

    /**
     * Frist, nach der ein `ATTEMPTING` ohne Fortschritt als steckengeblieben gilt.
     *
     * Deutlich ueber der Summe der Zeitlimits eines Versuchs (10 s je Verbindungsschritt, mal der
     * Zahl der Empfaenger), damit ein langsamer, aber laufender Versand nicht abgeraeumt wird.
     */
    const val STALE_ATTEMPT_MILLIS = 15 * 60 * 1000L

    /**
     * Holt einen steckengebliebenen Versuch **im laufenden Prozess** zurueck.
     *
     * Ohne diesen Uebergang bliebe ein Auftrag bis zum naechsten Prozessstart in `ATTEMPTING`
     * liegen: unsichtbar, weil nicht terminal, und unbearbeitet, weil nicht sendefaellig. Der Fall
     * entsteht real, wenn ein Empfaengerergebnis nicht haltbar geschrieben werden konnte oder die
     * Versand-Coroutine abgebrochen wurde.
     *
     * Ungefaehrlich, weil alle Versandlaeufe eines Prozesses serialisiert sind: Ist die Frist
     * abgelaufen, laeuft zu diesem Auftrag nachweislich kein Versand mehr.
     */
    fun onStaleAttempt(job: EmailForwardingJob, now: Long): EmailForwardingJob =
        if (job.state == EmailDeliveryState.ATTEMPTING &&
            now - job.updatedAtMillis >= STALE_ATTEMPT_MILLIS
        ) {
            job.copy(
                state = EmailDeliveryState.QUEUED,
                nextAttemptAtMillis = null,
                updatedAtMillis = now
            )
        } else {
            job
        }

    /** Ablauf-Scan: Ein nicht terminaler Auftrag jenseits der Frist wird terminal. */
    fun onExpiryScan(job: EmailForwardingJob, now: Long): EmailForwardingJob {
        if (job.state.isTerminal) return job
        if (!EmailRetryPolicy.isExpired(job.createdAtMillis, now)) return job
        return job.copy(
            state = if (job.deliveredCount > 0) EmailDeliveryState.PARTIAL else EmailDeliveryState.FAILED,
            nextAttemptAtMillis = null,
            updatedAtMillis = now
        )
    }

    /** Ein Auftrag, dessen Versand ansteht: frisch eingereiht oder faelliger Neuversuch. */
    fun isDispatchDue(job: EmailForwardingJob, now: Long): Boolean = when (job.state) {
        EmailDeliveryState.QUEUED -> true
        EmailDeliveryState.RETRY -> (job.nextAttemptAtMillis ?: 0L) <= now
        else -> false
    }

    private fun updateRecipient(
        job: EmailForwardingJob,
        now: Long,
        address: String,
        transform: (EmailRecipientState) -> EmailRecipientState
    ): EmailForwardingJob {
        val existing = job.recipients.firstOrNull { it.address == address } ?: return job
        val updated = transform(existing)
        if (updated == existing) return job
        return job.copy(
            recipients = job.recipients.map { if (it.address == address) updated else it },
            updatedAtMillis = now
        )
    }
}

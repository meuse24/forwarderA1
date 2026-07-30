package info.meuse24.smsforwarderneoA1.service

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import info.meuse24.smsforwarderneoA1.AppContainer
import info.meuse24.smsforwarderneoA1.LoggingManager
import info.meuse24.smsforwarderneoA1.R
import info.meuse24.smsforwarderneoA1.SnackbarManager
import info.meuse24.smsforwarderneoA1.data.local.QueueUpdate
import info.meuse24.smsforwarderneoA1.domain.model.ForwardingState
import info.meuse24.smsforwarderneoA1.domain.model.SmsCallbackUri
import info.meuse24.smsforwarderneoA1.domain.model.SmsDeliveryReducer
import info.meuse24.smsforwarderneoA1.domain.model.SmsPartResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import info.meuse24.smsforwarderneoA1.util.MmiCodeMasker

/**
 * Rueckmeldung des Telefonie-Frameworks zu einem gesendeten SMS-Teil.
 *
 * Fuehrt die Zustandsuebergaenge der Weiterleitungs-Queue aus. Die Zuordnung erfolgt ueber die
 * Daten-URI des PendingIntent - Extras zaehlen nicht zu dessen Identitaet und koennen die
 * Zuordnung nach einem Prozessneustart nicht tragen.
 */
class SmsSentReceiver : BroadcastReceiver() {

    companion object {
        /**
         * Prozessweit, nicht je Receiver-Instanz: Die Instanz ist nach `onReceive` nicht mehr
         * gueltig, die begonnene Arbeit laeuft aber noch (durch `goAsync` gedeckt).
         */
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /**
         * Nur die drei dokumentierten Funkfehler gelten als voruebergehend. Jeder andere Code
         * belegt zwar einen Fehlschlag, aber keinen, den ein Neuversand heilt - er wuerde nur
         * weitere kostenpflichtige Versuche erzeugen.
         */
        internal fun classify(resultCode: Int): SmsPartResult = when (resultCode) {
            Activity.RESULT_OK -> SmsPartResult.OK
            SmsManager.RESULT_ERROR_NO_SERVICE,
            SmsManager.RESULT_ERROR_RADIO_OFF,
            SmsManager.RESULT_ERROR_GENERIC_FAILURE -> SmsPartResult.TRANSIENT_FAILURE

            else -> SmsPartResult.TERMINAL_FAILURE
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val code = resultCode
        val result = classify(code)
        val recipient = intent.getStringExtra("recipient") ?: "unknown"
        val partIndex = intent.getIntExtra("part_index", 0)
        val totalParts = intent.getIntExtra("total_parts", 1)
        val partInfo = if (totalParts > 1) " (Teil ${partIndex + 1}/$totalParts)" else ""

        report(context, result, code, recipient, partIndex, totalParts, partInfo)

        val reference = SmsCallbackUri.parse(intent.dataString)
        if (reference == null) {
            // Kein Weiterleitungsvorgang (z. B. Test-SMS): nur protokollieren.
            return
        }

        val pendingResult = goAsync()
        scope.launch {
            try {
                val queue = AppContainer.getForwardingQueueSafe() ?: return@launch
                // Der Teilindex stammt aus der URI, nicht aus den Extras: Nur die URI gehoert zur
                // Identitaet des PendingIntent und traegt damit verlaesslich, welcher Teil gemeint
                // ist. Eine doppelt zugestellte Rueckmeldung bleibt dadurch folgenlos.
                when (val transition = queue.update(reference.operationId) { operation ->
                    SmsDeliveryReducer.onPartResult(
                        operation, reference.attempt, reference.partIndex, result, code,
                        System.currentTimeMillis()
                    )
                }) {
                    is QueueUpdate.Applied -> {
                        val updated = transition.operation
                        LoggingManager.logInfo(
                            component = "SmsSentReceiver",
                            action = "QUEUE_TRANSITION",
                            message = "Zustand nach Rueckmeldung aktualisiert",
                            details = mapOf(
                                "operation_id" to reference.operationId,
                                "attempt" to reference.attempt,
                                "part" to reference.partIndex,
                                "state" to updated.state.name,
                                "confirmed" to updated.confirmedParts,
                                "expected" to updated.expectedParts
                            )
                        )
                        // Der regulaere Scan laeuft nur alle fuenf Minuten; ein faelliger
                        // Neuversuch soll aber seinen eigenen Backoff einhalten.
                        if (updated.state == ForwardingState.RETRY) {
                            SmsForegroundService.requestQueueScan(context)
                        }
                    }

                    is QueueUpdate.Rejected -> LoggingManager.logDebug(
                        component = "SmsSentReceiver",
                        action = "QUEUE_CALLBACK_IGNORED",
                        message = "Rueckmeldung ohne Aussagewert - kein Zustandswechsel",
                        details = mapOf(
                            "operation_id" to reference.operationId,
                            "attempt" to reference.attempt,
                            "part" to reference.partIndex,
                            "state" to transition.operation.state.name
                        )
                    )

                    QueueUpdate.NotStored -> LoggingManager.logWarning(
                        component = "SmsSentReceiver",
                        action = "QUEUE_CALLBACK_UNMATCHED",
                        message = "Kein passender Vorgang oder Schreibfehler",
                        details = mapOf("operation_id" to reference.operationId)
                    )
                }
            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "SmsSentReceiver",
                    action = "QUEUE_UPDATE_FAILED",
                    message = "Zustandsuebergang konnte nicht geschrieben werden",
                    error = e
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun report(
        context: Context,
        result: SmsPartResult,
        resultCode: Int,
        recipient: String,
        partIndex: Int,
        totalParts: Int,
        partInfo: String
    ) {
        if (result == SmsPartResult.OK) {
            LoggingManager.logInfo(
                component = "SmsSentReceiver",
                action = "SMS_SENT_SUCCESS",
                message = "SMS erfolgreich gesendet$partInfo",
                details = mapOf(
                    "recipient" to MmiCodeMasker.maskNumber(recipient),
                    "part_index" to partIndex,
                    "total_parts" to totalParts
                )
            )
            if (partIndex == totalParts - 1) {
                SnackbarManager.showSuccess(context.getString(R.string.snackbar_sms_sent_success, recipient))
            }
            return
        }

        LoggingManager.logError(
            component = "SmsSentReceiver",
            action = "SMS_SENT_ERROR",
            message = "SMS-Sendefehler$partInfo",
            details = mapOf(
                "recipient" to MmiCodeMasker.maskNumber(recipient),
                "error_code" to resultCode,
                "classification" to result.name
            )
        )
        SnackbarManager.showError(
            when (resultCode) {
                SmsManager.RESULT_ERROR_GENERIC_FAILURE -> context.getString(R.string.snackbar_sms_error_generic)
                SmsManager.RESULT_ERROR_NO_SERVICE -> context.getString(R.string.snackbar_sms_error_no_service)
                SmsManager.RESULT_ERROR_NULL_PDU -> context.getString(R.string.snackbar_sms_error_null_pdu)
                SmsManager.RESULT_ERROR_RADIO_OFF -> context.getString(R.string.snackbar_sms_error_radio_off)
                else -> context.getString(R.string.snackbar_sms_error_code, resultCode)
            }
        )
    }
}

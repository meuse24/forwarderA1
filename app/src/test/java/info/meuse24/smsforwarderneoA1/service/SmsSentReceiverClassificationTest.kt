package info.meuse24.smsforwarderneoA1.service

import android.app.Activity
import android.telephony.SmsManager
import info.meuse24.smsforwarderneoA1.domain.model.SmsPartResult
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Die Zuordnung Plattform-Ergebniscode -> Wiederholungsentscheidung.
 *
 * Laeuft auf der JVM, weil die Ergebniscodes Konstanten sind und der Klassifikator keine
 * Android-Aufrufe macht.
 */
class SmsSentReceiverClassificationTest {

    @Test fun `RESULT_OK confirms a part`() {
        assertEquals(SmsPartResult.OK, SmsSentReceiver.classify(Activity.RESULT_OK))
    }

    @Test fun `the three documented radio failures are transient and justify a resend`() {
        assertEquals(
            SmsPartResult.TRANSIENT_FAILURE,
            SmsSentReceiver.classify(SmsManager.RESULT_ERROR_NO_SERVICE)
        )
        assertEquals(
            SmsPartResult.TRANSIENT_FAILURE,
            SmsSentReceiver.classify(SmsManager.RESULT_ERROR_RADIO_OFF)
        )
        assertEquals(
            SmsPartResult.TRANSIENT_FAILURE,
            SmsSentReceiver.classify(SmsManager.RESULT_ERROR_GENERIC_FAILURE)
        )
    }

    @Test fun `null pdu is terminal`() {
        assertEquals(
            SmsPartResult.TERMINAL_FAILURE,
            SmsSentReceiver.classify(SmsManager.RESULT_ERROR_NULL_PDU)
        )
    }

    @Test fun `an unknown code is terminal rather than repeatedly retried at the users cost`() {
        assertEquals(SmsPartResult.TERMINAL_FAILURE, SmsSentReceiver.classify(4711))
    }
}

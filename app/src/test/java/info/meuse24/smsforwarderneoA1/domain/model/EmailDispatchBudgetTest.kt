package info.meuse24.smsforwarderneoA1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailDispatchBudgetTest {

    @Test fun `the budget grows with the number of recipients`() {
        assertEquals(EmailDispatchBudget.PER_RECIPIENT_MILLIS, EmailDispatchBudget.forJob(1))
        assertEquals(3 * EmailDispatchBudget.PER_RECIPIENT_MILLIS, EmailDispatchBudget.forJob(3))
    }

    /** Ohne Deckel könnte eine lange Empfängerliste den Dienst beliebig lange wachhalten. */
    @Test fun `the budget is capped`() {
        assertEquals(EmailDispatchBudget.MAX_JOB_MILLIS, EmailDispatchBudget.forJob(1000))
        assertTrue(EmailDispatchBudget.forJob(100) <= EmailDispatchBudget.MAX_JOB_MILLIS)
    }

    @Test fun `an empty recipient list still gets one slot`() {
        assertEquals(EmailDispatchBudget.PER_RECIPIENT_MILLIS, EmailDispatchBudget.forJob(0))
    }

    /**
     * Der WakeLock muss den Versand ueberdauern: Geprueft wird erst nach der Rueckkehr eines
     * Einzelversands, der letzte darf das Budget also ueberschreiten.
     */
    @Test fun `the wake lock outlasts the budget by one recipient`() {
        listOf(1, 3, 1000).forEach { count ->
            assertTrue(
                EmailDispatchBudget.wakeLockMillisForJob(count) >= EmailDispatchBudget.forJob(count) +
                    EmailDispatchBudget.PER_RECIPIENT_MILLIS
            )
        }
    }

    @Test fun `a run can hold several jobs`() {
        assertTrue(EmailDispatchBudget.MAX_RUN_START_MILLIS > EmailDispatchBudget.MAX_JOB_MILLIS)
    }

    /**
     * Die Durchlaufgrenze ist eine **Startgrenze**: Ein laufender Auftrag wird zu Ende gefuehrt,
     * statt unter seinem eigenen WakeLock gekappt zu werden. Der Preis dafuer ist benannt und
     * begrenzt - ein Auftrag, der eine Millisekunde vor der Grenze beginnt, verlaengert den
     * Durchlauf um sein volles Budget plus einen Einzelversand.
     */
    @Test fun `the worst case run is the start limit plus one full job`() {
        assertEquals(
            EmailDispatchBudget.MAX_RUN_START_MILLIS +
                EmailDispatchBudget.MAX_JOB_MILLIS +
                EmailDispatchBudget.PER_RECIPIENT_MILLIS,
            EmailDispatchBudget.worstCaseRunMillis()
        )
    }

    /** Auch im schlechtesten Fall bleibt ein Durchlauf deutlich unter dem Scan-Takt mal drei. */
    @Test fun `the worst case run stays bounded`() {
        assertTrue(EmailDispatchBudget.worstCaseRunMillis() < 16 * 60 * 1000L)
    }
}

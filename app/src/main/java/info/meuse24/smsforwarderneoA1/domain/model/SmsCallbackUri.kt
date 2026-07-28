package info.meuse24.smsforwarderneoA1.domain.model

/**
 * Identitaet eines Sende-Callbacks.
 *
 * Extras zaehlen **nicht** zur Identitaet eines PendingIntent; zwei Callbacks, die sich nur in
 * ihren Extras unterscheiden, sind fuer die Plattform derselbe Intent. Die Zuordnung muss
 * deshalb in der Daten-URI stehen. Sie enthaelt auch die Versuchsnummer, damit verspaetete
 * Rueckmeldungen eines frueheren Sendeversuchs erkennbar sind.
 *
 * Als reine Zeichenkettenlogik gehalten, damit sie ohne Geraet pruefbar ist; `Uri.parse` wird
 * erst an der Plattformgrenze angewandt.
 */
object SmsCallbackUri {

    private const val PREFIX = "smsfwd://op/"

    data class Reference(val operationId: String, val attempt: Int, val partIndex: Int)

    fun build(operationId: String, attempt: Int, partIndex: Int): String =
        "$PREFIX$operationId/attempt/$attempt/part/$partIndex"

    fun parse(uri: String?): Reference? {
        if (uri == null || !uri.startsWith(PREFIX)) return null
        val segments = uri.removePrefix(PREFIX).split('/')
        if (segments.size != 5 || segments[1] != "attempt" || segments[3] != "part") return null
        val operationId = segments[0].takeIf { it.isNotBlank() } ?: return null
        val attempt = segments[2].toIntOrNull() ?: return null
        val partIndex = segments[4].toIntOrNull() ?: return null
        return Reference(operationId, attempt, partIndex)
    }

    /**
     * Request-Code deterministisch aus der URI. Kollisionsfreiheit ist keine Anforderung: Die
     * Daten-URI ist bereits Teil der PendingIntent-Identitaet. Ein prozesslokaler Zaehler waere
     * dagegen schaedlich, weil er nach einem Neustart wieder bei 0 beginnt.
     */
    fun requestCode(uri: String): Int = uri.hashCode()
}

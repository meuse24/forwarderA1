package info.meuse24.smsforwarderneoA1.domain.model

/**
 * Persistenter Hinweis darauf, dass Eintraege der Weiterleitungs-Queue verloren gegangen sind.
 *
 * Ein reiner Logeintrag genuegt hier nicht: Ein Totalverlust der Queue ist genau der Fall, in
 * dem sonst kommentarlos etwas verschwindet.
 *
 * @param lostEntries Anzahl verlorener Eintraege, oder [UNKNOWN_COUNT], wenn das Dokument als
 *   Ganzes unlesbar war und die Anzahl deshalb nicht ermittelbar ist.
 */
data class QueueCorruptionWarning(
    val timestampMillis: Long,
    val lostEntries: Int
) {
    companion object {
        const val UNKNOWN_COUNT = -1
    }
}

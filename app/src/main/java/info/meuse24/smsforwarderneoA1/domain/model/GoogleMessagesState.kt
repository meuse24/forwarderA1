package info.meuse24.smsforwarderneoA1.domain.model

/**
 * Zustand von Google Messages auf dem Geraet, soweit er ohne zusaetzliche Berechtigung
 * feststellbar ist.
 *
 * WICHTIG: Dieser Zustand sagt NICHTS darueber aus, ob RCS tatsaechlich aktiv ist.
 * Der RCS-Status ist fuer eine normale Drittanbieter-App nicht auslesbar
 * (siehe rcs.md, Anhang B). Die App darf ihn deshalb nie behaupten, sondern nur
 * erklaeren, dass RCS-Chats grundsaetzlich nicht weitergeleitet werden koennen.
 */
enum class GoogleMessagesState {
    /** Google Messages ist nicht installiert. Kein RCS-Risiko, kein Hinweis noetig. */
    NOT_INSTALLED,

    /**
     * Installiert, aber eine andere App ist Standard-SMS-App.
     * Die Rufnummer kann trotzdem noch serverseitig fuer RCS registriert sein -
     * genau die Konstellation, in der Nachrichten spurlos verschwinden.
     */
    INSTALLED_NOT_DEFAULT,

    /** Google Messages ist die Standard-SMS-App. RCS-Chats sind hier am wahrscheinlichsten. */
    DEFAULT_SMS_APP
}

/** Paketname von Google Messages - der einzige relevante RCS-Client auf Android. */
const val GOOGLE_MESSAGES_PACKAGE = "com.google.android.apps.messaging"

/**
 * Reine Abbildung der beiden Rohinformationen auf den Zustand.
 * Bewusst frei von Android-Abhaengigkeiten, damit sie ohne Emulator testbar ist.
 *
 * @param isInstalled Ergebnis der Paketpruefung (benoetigt den queries-Eintrag im Manifest)
 * @param defaultSmsPackage Rueckgabe von Telephony.Sms.getDefaultSmsPackage(), ggf. null
 */
fun resolveGoogleMessagesState(
    isInstalled: Boolean,
    defaultSmsPackage: String?
): GoogleMessagesState = when {
    defaultSmsPackage == GOOGLE_MESSAGES_PACKAGE -> GoogleMessagesState.DEFAULT_SMS_APP
    isInstalled -> GoogleMessagesState.INSTALLED_NOT_DEFAULT
    else -> GoogleMessagesState.NOT_INSTALLED
}

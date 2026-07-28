package info.meuse24.smsforwarderneoA1.domain.model

/** Keeps an operation's captured mode authoritative over settings changed later. */
object MmiExecutionModeResolver {
    fun resolve(capturedMode: MmiExecutionMode?, configuredMode: MmiExecutionMode): MmiExecutionMode =
        capturedMode ?: configuredMode
}

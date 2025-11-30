package info.meuse24.smsforwarderneoA1.util.sms

/**
 * GSM 7-Bit encoder for SMS messages.
 *
 * Encodes strings using the GSM 7-bit alphabet, handling both
 * standard and extended characters.
 */
object Gsm7BitEncoder {
    // GSM 7-Bit Alphabet: Enthält alle Standard-Zeichen, die in einer SMS verwendet werden können
    private val gsm7BitAlphabet = charArrayOf(
        '@', '£', '$', '¥', 'è', 'é', 'ù', 'ì', 'ò', 'Ç', '\n', 'Ø', 'ø', '\r', 'Å', 'å',
        'Δ', '_', 'Φ', 'Γ', 'Λ', 'Ω', 'Π', 'Ψ', 'Σ', 'Θ', 'Ξ', '\u001B', 'Æ', 'æ', 'ß', 'É',
        ' ', '!', '"', '#', '¤', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', ':', ';', '<', '=', '>', '?',
        '¡', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O',
        'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'Ä', 'Ö', 'Ñ', 'Ü', '§',
        '¿', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o',
        'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'ä', 'ö', 'ñ', 'ü', 'à'
    )

    // Erweiterte Zeichen: Benötigen 2 Bytes in der GSM 7-Bit-Kodierung
    private val gsm7BitExtendedChars = mapOf(
        '|' to 40, '^' to 20, '€' to 101, '{' to 40, '}' to 41,
        '[' to 60, '~' to 20, ']' to 61, '\\' to 47
    )

    /**
     * Kodiert einen String in GSM 7-Bit-Format.
     * @param input Der zu kodierende Eingabestring
     * @return Ein Paar bestehend aus dem kodierten String und der Länge der resultierenden SMS
     */
    fun encode(input: String): Pair<String, Int> {
        val sb = StringBuilder()
        var smsLength = 0

        input.forEach { char ->
            when {
                gsm7BitAlphabet.contains(char) -> {
                    sb.append(char)
                    smsLength++
                }

                gsm7BitExtendedChars.containsKey(char) -> {
                    sb.append('\u001B') // Escape-Zeichen
                    sb.append(gsm7BitAlphabet[gsm7BitExtendedChars[char] ?: 0])
                    smsLength += 2 // Erweiterte Zeichen zählen als 2
                }

                else -> {
                    // Ersetze nicht unterstützte Zeichen durch ein Leerzeichen
                    sb.append('_')
                    smsLength++
                }
            }
        }

        return Pair(sb.toString(), smsLength)
    }
}

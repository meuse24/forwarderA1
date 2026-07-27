package info.meuse24.smsforwarderneoA1.util

/** Masks the destination portion of MMI/USSD commands before they reach logs. */
object MmiCodeMasker {
    fun mask(code: String): String {
        val normalized = code.replace(Regex("[\\s-]"), "")
        val match = Regex("^(.*\\*)([+0-9]{5,})(\\*+|#)$").matchEntire(normalized)
        return if (match != null) {
            match.groupValues[1] + maskNumber(match.groupValues[2]) + match.groupValues[3]
        } else maskFreeText(normalized)
    }

    fun maskNumber(number: String): String {
        val digits = number.replace(Regex("[\\s-]"), "")
        return if (digits.length <= 4) "****" else digits.take(2) + "****" + digits.takeLast(2)
    }

    /** Fails closed for carrier-specific formats and free-form network replies. */
    fun maskFreeText(text: String): String =
        Regex("(?:\\+|00)?[0-9][0-9\\s-]{4,}[0-9]").replace(text) { maskNumber(it.value) }
}

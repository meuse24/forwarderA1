package info.meuse24.smsforwarderneoA1.domain.model

/** A named, complete set of call-forwarding control codes. */
enum class MmiCodeProfile {
    STANDARD_GSM,
    A1_SPECIAL,
    CUSTOM,
}

data class MmiCodeSet(
    val activatePrefix: String,
    val activateSuffix: String,
    val deactivateCode: String,
    val statusCode: String,
) {
    fun isValid(): Boolean = listOf(activatePrefix, activateSuffix, deactivateCode, statusCode)
        .all { it.isNotBlank() && it.all { char -> char in "*#0123456789" } } &&
        deactivateCode.first() in "*#" && statusCode.first() in "*#"
}

object MmiCodeProfiles {
    /** Documented A1/GSM registration form; works when no forwarding target is registered yet. */
    val standardGsm = MmiCodeSet("**21*", "#", "##21#", "*#21#")
    val a1Special = MmiCodeSet("*21*", "**", "**21**", "*021**")

    fun codesFor(profile: MmiCodeProfile): MmiCodeSet? = when (profile) {
        MmiCodeProfile.STANDARD_GSM -> standardGsm
        MmiCodeProfile.A1_SPECIAL -> a1Special
        MmiCodeProfile.CUSTOM -> null
    }

    fun detect(codes: MmiCodeSet): MmiCodeProfile = when (codes) {
        standardGsm -> MmiCodeProfile.STANDARD_GSM
        a1Special -> MmiCodeProfile.A1_SPECIAL
        else -> MmiCodeProfile.CUSTOM
    }
}

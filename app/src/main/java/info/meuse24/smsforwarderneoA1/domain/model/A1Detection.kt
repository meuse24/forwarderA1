package info.meuse24.smsforwarderneoA1.domain.model

enum class A1Detection { A1_CONFIRMED, NOT_A1, UNKNOWN }

/** Conservative SIM-provider classifier. Unknown is preferable to a false A1 recommendation. */
object A1Detector {
    // Android's AOSP carrier list: canonical_id 14, MCC/MNC 23201 (checked 2026-07-28).
    private const val A1_AUSTRIA_CARRIER_ID = 14
    // A1 Austria's primary public SIM network code. Do not use the currently registered network.
    private const val A1_MCC_MNC = "23201"

    fun detect(carrierId: Int?, mccMnc: String?, carrierName: String?, displayName: String?): A1Detection {
        if (carrierId == A1_AUSTRIA_CARRIER_ID && mccMnc?.startsWith("232") == true) return A1Detection.A1_CONFIRMED
        if (mccMnc == A1_MCC_MNC) return A1Detection.A1_CONFIRMED
        // A display name such as "A1" is not country-specific and must never trigger the dialog.
        if (mccMnc?.startsWith("232") == true) return A1Detection.NOT_A1
        return A1Detection.UNKNOWN
    }
}

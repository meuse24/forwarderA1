package info.meuse24.smsforwarderneoA1.domain.model

/** Evidence level for the carrier-side call forwarding operation. */
enum class ForwardingVerification {
    NOT_CHECKED,
    ASSUMED_SUCCESS,
    UNKNOWN_NO_RESPONSE,
    CONFIRMED_SUCCESS,
    DIAL_FAILED,
    USER_REPORTED_FAILURE
}

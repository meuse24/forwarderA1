package info.meuse24.smsforwarderneoA1.domain.model

/** Mechanism used to hand the carrier command to Android. */
enum class DialPath { USSD_REQUEST, TELECOM_MANAGER, ACTION_CALL, NOT_DISPATCHED }

package info.meuse24.smsforwarderneoA1.domain.model

/** Pure decision part of the one-time profile materialization migration. */
object MmiProfileMigration {
    fun defaultsFor(hasAnyMmiKey: Boolean, isUpdate: Boolean, forwardingActive: Boolean): MmiCodeSet =
        if (hasAnyMmiKey || isUpdate || forwardingActive) MmiCodeProfiles.a1Special else MmiCodeProfiles.standardGsm

    fun materialize(existing: MmiCodeSet, fallback: MmiCodeSet): MmiCodeSet = MmiCodeSet(
        existing.activatePrefix.ifBlank { fallback.activatePrefix },
        existing.activateSuffix.ifBlank { fallback.activateSuffix },
        existing.deactivateCode.ifBlank { fallback.deactivateCode },
        existing.statusCode.ifBlank { fallback.statusCode },
    )
}

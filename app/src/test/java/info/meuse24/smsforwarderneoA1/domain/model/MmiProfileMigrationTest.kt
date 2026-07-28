package info.meuse24.smsforwarderneoA1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MmiProfileMigrationTest {
    @Test fun newInstallationUsesStandardProfile() {
        assertEquals(MmiCodeProfiles.standardGsm, MmiProfileMigration.defaultsFor(false, false, false))
    }

    @Test fun updateWithoutMmiKeysKeepsLegacyProfile() {
        assertEquals(MmiCodeProfiles.a1Special, MmiProfileMigration.defaultsFor(false, true, false))
    }

    @Test fun activeForwardingKeepsLegacyProfileWhenInstallStateIsUnclear() {
        assertEquals(MmiCodeProfiles.a1Special, MmiProfileMigration.defaultsFor(false, false, true))
    }

    @Test fun partialLegacySettingsAreMaterializedWithoutChangingSavedValues() {
        val partial = MmiCodeSet("*21*", "", "", "")
        assertEquals(MmiCodeProfiles.a1Special, MmiProfileMigration.materialize(partial, MmiCodeProfiles.a1Special))
    }
    @Test fun materializationIsIdempotent() {
        val once = MmiProfileMigration.materialize(MmiCodeProfiles.a1Special, MmiCodeProfiles.standardGsm)
        assertEquals(once, MmiProfileMigration.materialize(once, MmiCodeProfiles.standardGsm))
    }
}

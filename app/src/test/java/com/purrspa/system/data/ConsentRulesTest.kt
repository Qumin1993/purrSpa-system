package com.purrspa.system.data

import org.junit.Assert.*
import org.junit.Test

class ConsentRulesTest {
    private fun cat(photo: Boolean = false, social: Boolean = false) = Cat(
        id = "cat", clientId = "client", name = "Test cat",
        photoConsent = photo, socialConsent = social
    )

    @Test fun permissionsDefaultToDenied() {
        assertFalse(ConsentRules.mayCapturePhoto(cat()))
        assertFalse(ConsentRules.mayPublishPhoto(cat()))
    }

    @Test fun photoPermissionDoesNotImplyPublication() {
        assertTrue(ConsentRules.mayCapturePhoto(cat(photo = true)))
        assertFalse(ConsentRules.mayPublishPhoto(cat(photo = true)))
    }

    @Test fun publicationRequiresBothPermissions() {
        assertFalse(ConsentRules.mayPublishPhoto(cat(social = true)))
        assertTrue(ConsentRules.mayPublishPhoto(cat(photo = true, social = true)))
    }

    @Test fun withdrawingPhotoPermissionRevokesEffectivePublication() {
        assertFalse(ConsentRules.effectiveSocialConsent(false, true))
    }
}

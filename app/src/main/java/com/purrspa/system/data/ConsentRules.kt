package com.purrspa.system.data

/** Consent is opt-in. Social sharing always requires permission to photograph. */
object ConsentRules {
    fun effectiveSocialConsent(photoConsent: Boolean, socialConsent: Boolean): Boolean =
        photoConsent && socialConsent

    fun mayCapturePhoto(cat: Cat): Boolean = cat.photoConsent

    fun mayPublishPhoto(cat: Cat): Boolean =
        effectiveSocialConsent(cat.photoConsent, cat.socialConsent)
}

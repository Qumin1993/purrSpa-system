# Purr Spa System — canonical V1 product plan
Updated: 2026-10-08. This plan supersedes the previous product scope. The client's 17-screen brief is the source of truth. Existing code is not evidence of feature completion.

## Product and UX
Simple English-only native Android application for **tablet and phone** to manage grooming clients, cats and visits. Large touch-friendly buttons, few taps, minimal typing, clear quick-select ratings and tags, calm Purr Spa branding inspired by supplied references (no copied photos). Tablet-first with genuinely usable phone layouts. Offline-first, locally stored private records.

**Primary workflow:** owner intake before appointment -> review cat profile/history -> create or open salon/mobile visit -> groomer records observed handling and treatment, before/after photos and notes -> review visit summary -> generate attractive client-facing PDF -> share by WhatsApp/email or save PDF/copy summary -> retain full per-cat visit history.

## Owner intake versus groomer observations
These must be **separate forms and separately labelled data**, never silently conflated:
- **Client/owner form (before visit):** owner name/contact/address; cat identity, breed, age/DOB where known, sex/neutered where known; health conditions and owner-reported symptoms, medication, allergies, sensitivities; past grooming, mats/fleas, behaviour and triggers; bathing/dryer/brushing/nails tolerance as reported by owner; handling advice; declarations and separate photo/social permissions with source, date and withdrawal history. Do not diagnose.
- **Groomer form (during/after visit):** actual observations for brushing, bathing, dryer, nails, paws, belly, tail, aggression/reactivity, sensitive spots, mats/fleas/coat condition, interventions, what worked, what to avoid next time, recommendations. Fast tap ratings e.g. OK / Sensitive / Difficult / Not tested plus optional text.
- **Intake completion V1:** owner can fill the intake on the salon tablet/phone using an owner-facing handover mode, or staff can record answers. A remotely sent self-service link is **not** included in offline V1; requires separately approved online service/sync and is not to be silently claimed. Consent records must identify whether staff or owner entered them; a checkbox alone is not independent proof of authorization.

## Required screens: exact client checklist
1. **Home Dashboard** — today's visits, counts of clients/cats, prominent New Visit button.
2. **Clients** — searchable owner list with contact details and linked cats.
3. **Cats** — searchable cat directory.
4. **New Client Form** — owner and cat identity/contact details, with ability to link multiple cats to an owner.
5. **Health & Behaviour Form** — owner-provided health, medication, previous grooming, behaviour, handling and permissions before visit.
6. **Visit Details** — salon visit/mobile home visit, date/time, owner address or visit address, travel fee and selected treatment/service.
7. **Groomer Form** — fast touch ratings for brushing, bathing, dryer, nails, paws, belly, tail, aggression, sensitivity; coat/mats/fleas, what works, optional notes.
8. **Visit Summary** — combined read-only visit overview distinguishing owner-reported intake from groomer-observed findings, service and travel charge, with editing entry points.
9. **Cat Profile** — identity, health warnings, prominent quick tags and practical 'next visit' handling guidance.
10. **Visit History** — all previous visits per cat, chronological, with assessments, treatments, photos and reports accessible.
11. **Photos** — private before/after capture or import, labelled and attached to the correct cat and visit; permission-aware.
12. **Notes** — private groomer notes about a specific cat/visit, separate from shareable summary.
13. **Generate PDF** — attractive branded, accurate client-facing visit report, with preview and privacy-aware contents.
14. **Send to Client** — share PDF through Android chooser (WhatsApp/email when installed), save/export PDF, copy summary text; do not claim delivery automatically.
15. **Calendar** — salon/mobile appointments with clear date and visit status.
16. **Services** — treatment list, understandable names and prices.
17. **Settings** — salon details, defaults and secure local data management.

The 17 screens may be steps, tabs or nested screens where that reduces taps, but all 17 named capabilities must be clearly accessible and acceptance-tested.

## Quick tags and practical history
At minimum: **Spicy, Senior, Fleas, Mats, Sensitive Belly**; also support configurable/extended tags. Show high-priority warnings on the cat profile and before starting a visit. Each past visit preserves its own observations, and the profile surfaces a concise most-recent/important 'what works best / avoid' summary, not just a list of dates. Owner-reported and groomer-observed tags should remain attributable.

## Data model / correctness
Clients can own multiple cats. Each visit must link to a cat and owner; V1 does not require a multi-cat booking in one appointment. Persist distinct owner intake, grooming assessment, cat profile, visit, service, visit photo, note, consent history and report metadata. Store money in GBP pence, timestamps as instants rendered in Europe/London, validate dates and prevent overlapping appointments. Store photos privately, not as Room blobs; avoid exposing private notes in client reports by default. Consent-gate public/social photo use; do not equate grooming photos with publication permission. Reliable drafts/autosave and recovery after process death are required for intake and groomer forms. Keep report/share operations safe with Android FileProvider. Preserve data across app updates via explicit tested Room migrations.

## Out of scope for client V1
Remote intake links, client accounts, cloud sync, automatic WhatsApp/email delivery, online booking, route optimization, subscriptions, advanced accounting, deposit/payment ledger, and multiple cats in a single visit. These are not blockers to the client's 17 requirements. Existing basic payment/deposit features may remain but must not distract from the required workflow. Encrypted export/backup with tested restore is a release safety requirement for locally stored customer data, not an extra user-facing product module.

## Implementation priorities
- **P0: build safety** — green GitHub Android CI, debug APK, regression unit tests; keep CI green.
- **P1: owner intake** — New Client Form and Health & Behaviour Form, separate owner declarations, validation, consent history, durable drafts and handover-friendly mode.
- **P2: groomer workflow** — full quick-tap Groomer Form, tags, practical cat profile summary, history and visit summary.
- **P3: photos and reports** — private before/after photos, attractive PDF preview, share/save/copy summary, privacy checks.
- **P4: polish all 17 screens** — phone/tablet adaptive layout, large controls, minimal typing, English text, services/settings/calendar completeness.
- **P5: data safety and release** — encrypted backup/verified restore, migration tests, accessibility, offline and device E2E, permission and privacy review.
Work incrementally; compile and run tests after every meaningful code change. Do not treat a successful compilation as proof of end-to-end usability.

## Acceptance tests
1. On both tablet and phone, create a new owner and cat; owner enters health/behaviour history and consents before visit without seeing groomer-private notes.
2. Open cat profile; see health cautions, quick tags and prior grooming advice at a glance.
3. Schedule a mobile home visit with correct address, time and travel fee; also schedule a salon visit.
4. During grooming, tap ratings for brushing, bathing, dryer, nails, paws, belly, tail and aggression, record sensitive belly and what worked, and add private notes.
5. Capture/import before and after photos for the correct cat/visit; verify privacy and permissions.
6. Kill/restart the app midway through owner and groomer forms and recover drafts without data loss.
7. View an accurate summary and chronological cat history, including observations and previous visits.
8. Generate a readable branded PDF without private notes by default; preview, share to an available WhatsApp/email target, save PDF and copy summary text.
9. Verify all 17 screens, large buttons, English-only text, tablet and phone layouts, offline operation, invalid data and duplicate taps.
10. Export an encrypted backup, restore on a clean install, verify cats/owners/visits/photos/consents, and test migration from earlier DB versions.

## Implementation tracking
See docs/IMPLEMENTATION_STATUS.md for actual delivered versus missing features. Never interpret this plan as proof that implementation is complete. No real customer data until backup, migrations, consent, security and recovery are verified.

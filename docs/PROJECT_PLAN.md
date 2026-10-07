# Purr Spa System — Android tablet application

Status: Phase 0 started. This document is the canonical handoff for continuing work on a computer.

## Product
Native Android tablet-first, offline-first salon management app for Purr Spa cat grooming. Design references provided in ChatGPT conversation on 2026-10-07 are inspiration, NOT reusable cat photographs. Create original/licensed decorative cat assets; never commit client photographs or real client information. UK location, GBP, Europe/London timezone, UK date formats. UI language English initially.

## Scope V1
- Dashboard with daily schedule, counts and pending actions.
- Client records and addresses; one client has multiple cats.
- Cat profiles: species/breed, sex, neuter status, DOB, weight, handling tags, health/medications and consent.
- Salon/mobile appointments, travel charges, services, deposits, cancellations/no-shows, overlap validation.
- Groomer assessments (separate pre-visit owner declarations from observed behavior), photos, visit notes, recommendations.
- Visit history and reports with PDF generation, share via Android Sharesheet/FileProvider; never claim WhatsApp delivery.
- Settings, local encrypted backup/export and verified restore, reminders where feasible.
- Data protection: minimal personal data, no PII in logs, deletion/export, explicit photo/social consent.

## Non-goals V1
Cloud sync, customer accounts, automatic WhatsApp sending, route optimization, subscriptions and online booking. Plan for future sync but do not implement it prematurely.

## Stack
Kotlin, Jetpack Compose, Material 3, MVVM/UDF, Navigation Compose, Room, DataStore, Hilt, Coroutines/Flow, WorkManager, JUnit and Android instrumentation tests. Use Gradle Kotlin DSL and version catalog. Compile/target SDK to be verified against installed toolchain. Android tablet adaptive layout; phone layouts as secondary.

## Architecture invariants
1. Room is the source of truth; UI observes flows and does not own durable data.
2. Screen ViewModels expose immutable UiState and events; collect with lifecycle-aware APIs.
3. SavedStateHandle stores identifiers/navigation state, not entire forms. Autosave drafts to Room on field changes (debounce where safe), flush on navigation; do not rely on onStop for correctness.
4. Foreign keys and transactions enforce referential integrity; stable UUIDs; UTC instants persisted, Europe/London rendered; GBP in integer pence.
5. Visit and service prices are snapshots, not linked to mutable current prices.
6. A booking can contain multiple cats; per-cat grooming assessment and report belong to the visit-cat relation.
7. Explicit appointment state machine: DRAFT, SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW. PDF versions and share attempts are separate records.
8. No destructive Room migrations in production. Migration tests on realistic fixtures.
9. Photo files stored privately with stable metadata and orphan cleanup; avoid storing full bitmaps in Room.
10. Idempotent background jobs; atomic export/backup writes; encrypted off-device backup and restore tests.

## Domain entities
Client, ClientAddress, Cat, CatHealth, CatHandlingPreference, Consent, Appointment, AppointmentCat, Service, AppointmentService, GroomingAssessment, VisitPhoto, GroomingReport, Payment, Reminder, AuditEvent. Avoid mixing nullable owner-entered history with groomer observations. No medical diagnosis claims.

## UI
Black sidebar, warm white/cream background, restrained gold and pink accents. Accessibility: 48dp touch targets, screen reader labels, dynamic font sizing, landscape and split screen. Navigation: Dashboard, Clients, Cats, Visits, Calendar, Notes, Services, Reports, Settings. Design cat images are decorative and should not be mistaken for actual customer cats. Prefer vector shapes for ornaments and optimized WebP for photos. Asset licensing/provenance documented.

## Implementation phases
P0: repository plan, Android skeleton, build config, theme, navigation, placeholder screens, CI.
P1: Room schema, repositories, migrations, seed-only fake/demo data, unit tests.
P2: clients/cats CRUD, intake wizard, privacy/consent.
P3: services, bookings, calendar, salon/mobile charges, conflicts.
P4: groomer assessment, autosave, notes, photos.
P5: visit summary, versioned PDF, export/share.
P6: encrypted backup/restore, reminders, settings, privacy tools.
P7: instrumentation and device E2E, accessibility, performance, security review and release.
Use a dedicated implementation pass and a final read-only audit, then fix findings and narrow recheck.

## Acceptance E2E
Create owner with two cats; schedule mobile booking and travel fee; groom one cat and save assessment/photo; force-stop/process-kill/relaunch and recover saved draft; complete booking; generate accurate PDF; share with external app; export encrypted backup; restore on fresh installation; confirm all links and files. Also test rotation, backgrounding, duplicate taps, no storage, corrupt backup, missing share target, DB migration and appointment overlap.

## Current status / next actions
- GitHub repository created by user; implementation initiated from ChatGPT.
- First commit should contain this plan and bootstrap.
- On desktop: clone, open in Android Studio, verify Gradle sync and installed SDK, run assembleDebug/testDebugUnitTest, then proceed P1.
- Do not claim build or tests passed until executed in a real Android toolchain.
- Screenshots are visual reference only. Original art/photography must be sourced/generated separately and committed with provenance.

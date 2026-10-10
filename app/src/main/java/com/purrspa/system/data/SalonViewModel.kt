package com.purrspa.system.data

import android.app.Application
import android.net.Uri
import java.io.File
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class SalonViewModel(app: Application) : AndroidViewModel(app) {
    private val db = PurrDatabase.get(app)
    private val formSaveMutex = Mutex()
    private val formRevision = AtomicLong()
    private val latestFormRevisions = ConcurrentHashMap<String, Long>()
    private val _bookingError = MutableStateFlow<String?>(null)
    val bookingError = _bookingError.asStateFlow()
    private val _photoMessage = MutableStateFlow<String?>(null)
    val photoMessage = _photoMessage.asStateFlow()
    fun clearPhotoMessage() { _photoMessage.value = null }
    fun clearBookingError() { _bookingError.value = null }
    val clients = db.clients().observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val cats = db.cats().observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val assessments = db.assessments().observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val visitPhotos = db.visitPhotos().observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val ownerIntakes = db.ownerIntakes().observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val consentEvents = db.consentEvents().observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val visits = db.visits().observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun addClient(name: String, phone: String, email: String, address: String) {
        if (name.isBlank()) return
        viewModelScope.launch { db.clients().insert(Client(UUID.randomUUID().toString(), name.trim(), phone.trim(), email.trim(), address.trim())) }
    }
    fun addCat(clientId: String, name: String, breed: String, notes: String, sex: String = "", dateOfBirth: String = "", neutered: Boolean = false, healthNotes: String = "") {
        if (clientId.isBlank() || name.isBlank()) return
        viewModelScope.launch { db.cats().insert(Cat(UUID.randomUUID().toString(), clientId, name.trim(), breed.trim(), notes.trim(), sex.trim(), dateOfBirth.trim(), neutered, healthNotes.trim())) }
    }
    fun updateClient(item: Client) {
        if (item.name.isBlank()) return
        viewModelScope.launch { db.clients().update(item) }
    }
    fun updateCat(item: Cat) {
        if (item.name.isBlank()) return
        viewModelScope.launch { db.cats().updateProfile(item.id, item.name.trim(), item.breed.trim(), item.notes.trim(), item.sex, item.dateOfBirth, item.neutered, item.healthNotes.trim()) }
    }
    fun updateCatHandlingTags(catId: String, tags: Set<String>) {
        val allowed = setOf("Spicy", "Senior", "Fleas", "Mats", "Sensitive belly")
        if (catId.isBlank() || !allowed.containsAll(tags)) return
        viewModelScope.launch {
            db.cats().updateHandlingTags(catId, allowed.filter { it in tags }.joinToString("|"))
        }
    }
    fun updateCatConsents(cat: Cat, photoAllowed: Boolean, socialAllowed: Boolean, source: String) {
        if (source !in setOf("OWNER_VERBAL", "OWNER_WRITTEN", "STAFF_RECORDED")) return
        val safeSocial = ConsentRules.effectiveSocialConsent(photoAllowed, socialAllowed)
        viewModelScope.launch {
            db.recordConsent(ConsentEvent(UUID.randomUUID().toString(), cat.id, photoAllowed, safeSocial, System.currentTimeMillis(), source))
        }
    }
    fun addVisit(catId: String, startMillis: Long, service: String, location: String, pricePence: Long) {
        if (catId.isBlank() || service.isBlank() || pricePence < 0) return
        if (!AppointmentRules.validNewStart(startMillis, System.currentTimeMillis())) {
            _bookingError.value = "Choose a future appointment date and time."
            return
        }
        _bookingError.value = null
        viewModelScope.launch {
            val inserted = db.insertVisitIfFree(
                Visit(UUID.randomUUID().toString(), catId, startMillis, service.trim(), location, pricePence)
            )
            if (!inserted) {
                _bookingError.value = "Appointment overlaps an existing booking (estimated 2 hours)."
            } else {
                _bookingError.value = null
            }
        }
    }
    fun deleteVisitPhoto(photo: VisitPhoto) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val removed = db.visitPhotos().deleteById(photo.id)
            if (removed > 0) File(getApplication<Application>().filesDir, "visit_photos/${photo.privateFilename}").delete()
        }
    }
    fun importVisitPhoto(visitId: String, kind: String, uri: Uri) {
        if (kind !in setOf("BEFORE", "AFTER")) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val visit = db.visits().get(visitId)
            if (visit == null || db.cats().get(visit.catId)?.photoConsent != true) {
                _photoMessage.value = "Owner photo consent is required before adding photos."
                return@launch
            }
            val app = getApplication<Application>()
            val id = UUID.randomUUID().toString()
            val dir = File(app.filesDir, "visit_photos").apply { mkdirs() }
            var target: File? = null
            try {
                val mime = app.contentResolver.getType(uri)
                val extension = when (mime) {
                    "image/jpeg" -> "jpg"
                    "image/png" -> "png"
                    "image/webp" -> "webp"
                    else -> {
                        _photoMessage.value = "Choose a JPEG, PNG or WebP image."
                        return@launch
                    }
                }
                val file = File(dir, "$id.$extension")
                target = file
                val stream = app.contentResolver.openInputStream(uri)
                if (stream == null) {
                    _photoMessage.value = "Cannot open selected photo."
                    return@launch
                }
                stream.use { input ->
                    file.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var total = 0L
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            if (total > 15_000_000) {
                                _photoMessage.value = "Photo exceeds the 15 MB limit."
                                return@launch
                            }
                            output.write(buffer, 0, count)
                        }
                        if (total == 0L) {
                            _photoMessage.value = "Selected photo is empty."
                            return@launch
                        }
                    }
                }
                val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                android.graphics.BitmapFactory.decodeFile(file.absolutePath, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                    _photoMessage.value = "Selected file is not a readable image."
                    return@launch
                }
                if (db.cats().get(visit.catId)?.photoConsent != true) {
                    _photoMessage.value = "Photo consent was withdrawn. Import cancelled."
                    return@launch
                }
                db.visitPhotos().insert(VisitPhoto(id, visitId, kind, file.name, System.currentTimeMillis()))
                target = null
                _photoMessage.value = "${kind.lowercase().replaceFirstChar { it.uppercase() }} photo added."
            } catch (_: Exception) {
                _photoMessage.value = "Could not import photo. Please try again."
            } finally {
                target?.delete()
            }
        }
    }
    fun saveOwnerIntake(item: OwnerIntake) {
        val valid = setOf("UNKNOWN", "OK", "SENSITIVE", "DIFFICULT")
        if (listOf(item.brushingTolerance, item.bathingTolerance, item.dryerTolerance, item.nailsTolerance).any { it !in valid }) return
        if (item.source !in setOf("OWNER_ENTERED", "STAFF_RECORDED")) return
        val key = "intake:${item.catId}"
        val revision = formRevision.incrementAndGet()
        latestFormRevisions[key] = revision
        viewModelScope.launch {
            formSaveMutex.withLock {
                if (latestFormRevisions[key] == revision && db.cats().get(item.catId) != null) {
                    db.ownerIntakes().upsert(item.copy(updatedMillis = System.currentTimeMillis()))
                }
            }
        }
    }
    fun saveAssessment(item: GroomingAssessment) {
        if (listOf(item.brushing, item.bathing, item.drying, item.nailTrim, item.paws, item.belly, item.tail).any { it !in -1..4 }) return
        val key = "assessment:${item.visitId}"
        val revision = formRevision.incrementAndGet()
        latestFormRevisions[key] = revision
        viewModelScope.launch {
            formSaveMutex.withLock {
                if (latestFormRevisions[key] == revision) {
                    db.assessments().upsert(item.copy(updatedMillis = System.currentTimeMillis()))
                }
            }
        }
    }
    fun setVisitCharges(visit: Visit, travelPence: Long, depositPence: Long) {
        if (!VisitBalance.validCharges(visit.pricePence, travelPence, depositPence)) return
        viewModelScope.launch { db.visits().updateCharges(visit.id, travelPence, depositPence) }
    }
    fun setVisitPayment(visit: Visit, status: String, method: String) {
        if (status !in setOf("UNPAID", "PAID")) return
        if (status == "PAID" && method !in setOf("CASH", "CARD", "BANK_TRANSFER", "OTHER")) return
        if (!VisitBalance.paymentStatusAllowed(visit.status, status)) return
        viewModelScope.launch { db.visits().updatePayment(visit.id, status, if (status == "PAID") method else "") }
    }
    fun saveVisitNotes(visit: Visit, notes: String) {
        viewModelScope.launch { db.visits().updateNotes(visit.id, notes.trim()) }
    }
    fun setVisitStatus(visit: Visit, status: String) {
        val allowed = mapOf("SCHEDULED" to setOf("IN_PROGRESS", "CANCELLED", "NO_SHOW"), "IN_PROGRESS" to setOf("COMPLETED", "CANCELLED"))
        if (status !in allowed[visit.status].orEmpty()) return
        viewModelScope.launch { db.visits().transitionStatus(visit.id, visit.status, status) }
    }
}

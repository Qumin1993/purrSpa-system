package com.purrspa.system.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class SalonViewModel(app: Application) : AndroidViewModel(app) {
    private val db = PurrDatabase.get(app)
    private val _bookingError = MutableStateFlow<String?>(null)
    val bookingError = _bookingError.asStateFlow()
    fun clearBookingError() { _bookingError.value = null }
    val clients = db.clients().observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val cats = db.cats().observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val assessments = db.assessments().observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
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
        viewModelScope.launch { db.cats().update(item) }
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
    fun saveAssessment(item: GroomingAssessment) {
        if (listOf(item.brushing, item.bathing, item.drying, item.nailTrim, item.paws, item.belly, item.tail).any { it !in -1..4 }) return
        viewModelScope.launch { db.assessments().upsert(item.copy(updatedMillis = System.currentTimeMillis())) }
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

package com.purrspa.system.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class SalonViewModel(app: Application) : AndroidViewModel(app) {
    private val db = PurrDatabase.get(app)
    val clients = db.clients().observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val cats = db.cats().observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val visits = db.visits().observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun addClient(name: String, phone: String, email: String, address: String) {
        if (name.isBlank()) return
        viewModelScope.launch { db.clients().insert(Client(UUID.randomUUID().toString(), name.trim(), phone.trim(), email.trim(), address.trim())) }
    }
    fun addCat(clientId: String, name: String, breed: String, notes: String) {
        if (clientId.isBlank() || name.isBlank()) return
        viewModelScope.launch { db.cats().insert(Cat(UUID.randomUUID().toString(), clientId, name.trim(), breed.trim(), notes.trim())) }
    }
    fun addVisit(catId: String, startMillis: Long, service: String, location: String, pricePence: Long) {
        if (catId.isBlank() || service.isBlank() || pricePence < 0) return
        viewModelScope.launch { db.visits().insert(Visit(UUID.randomUUID().toString(), catId, startMillis, service.trim(), location, pricePence)) }
    }
    fun saveVisitNotes(visit: Visit, notes: String) {
        viewModelScope.launch { db.visits().update(visit.copy(notes = notes.trim())) }
    }
    fun setVisitStatus(visit: Visit, status: String) {
        val allowed = mapOf("SCHEDULED" to setOf("IN_PROGRESS", "CANCELLED", "NO_SHOW"), "IN_PROGRESS" to setOf("COMPLETED", "CANCELLED"))
        if (status !in allowed[visit.status].orEmpty()) return
        viewModelScope.launch { db.visits().update(visit.copy(status = status)) }
    }
}

package com.purrspa.system

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.purrspa.system.reports.VisitPdf
import androidx.compose.ui.unit.dp
import com.purrspa.system.data.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SalonScreen(page: String, vm: SalonViewModel) {
    val context = LocalContext.current
    val bookingError by vm.bookingError.collectAsStateWithLifecycle()
    val clients by vm.clients.collectAsStateWithLifecycle()
    val cats by vm.cats.collectAsStateWithLifecycle()
    val visits by vm.visits.collectAsStateWithLifecycle()
    val assessments by vm.assessments.collectAsStateWithLifecycle()
    var showAdd by remember(page) { mutableStateOf(false) }
    var editingVisit by remember { mutableStateOf<Visit?>(null) }
    var assessingVisit by remember { mutableStateOf<Visit?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(page, style = MaterialTheme.typography.headlineLarge)
        when(page) {
            "Home" -> {
                Text("Clients: ${clients.size}    Cats: ${cats.size}    Visits: ${visits.size}")
                Text("Upcoming appointments", style = MaterialTheme.typography.titleLarge)
                visits.filter { it.startMillis >= System.currentTimeMillis() && it.status == "SCHEDULED" }.sortedBy { it.startMillis }.take(8).forEach {
                    Text("${cats.firstOrNull { c -> c.id == it.catId }?.name ?: "Unknown cat"} • ${it.service} • ${dateTime(it.startMillis)}")
                }
            }
            "Clients" -> {
                Button(onClick = { showAdd = true }) { Text("Add client") }
                clients.forEach { Text("${it.name}  •  ${it.phone}  •  ${it.email}") }
            }
            "Cats" -> {
                Button(onClick = { showAdd = true }, enabled = clients.isNotEmpty()) { Text("Add cat") }
                cats.forEach { Text("${it.name} • ${it.breed} • Owner: ${clients.firstOrNull { c -> c.id == it.clientId }?.name ?: "Unknown"}") }
            }
            "Visits", "Calendar" -> {
                Button(onClick = { showAdd = true }, enabled = cats.isNotEmpty()) { Text("New visit") }
                visits.sortedByDescending { it.startMillis }.forEach { visit ->
                    Card {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text("${cats.firstOrNull { it.id == visit.catId }?.name ?: "Unknown"} • ${visit.service}", style = MaterialTheme.typography.titleMedium)
                            Text("${dateTime(visit.startMillis)} • ${visit.location} • £${"%.2f".format(Locale.UK, visit.pricePence / 100.0)} • ${visit.status}")
                            if (visit.notes.isNotBlank()) Text("Notes: ${visit.notes}")
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { editingVisit = visit }) { Text("Edit notes") }
                                TextButton(onClick = { assessingVisit = visit }) { Text("Groomer form") }
                                val reportCat = cats.firstOrNull { it.id == visit.catId }
                                val reportOwner = clients.firstOrNull { it.id == reportCat?.clientId }
                                TextButton(enabled = reportCat != null && reportOwner != null, onClick = {
                                    if (reportCat != null && reportOwner != null) {
                                        runCatching { VisitPdf.share(context, visit, reportCat, reportOwner, assessments.firstOrNull { it.visitId == visit.id }) }
                                            .onFailure { Toast.makeText(context, "Could not share report", Toast.LENGTH_LONG).show() }
                                    }
                                }) { Text("Share PDF") }
                                if (visit.status == "SCHEDULED") {
                                    TextButton(onClick = { vm.setVisitStatus(visit, "IN_PROGRESS") }) { Text("Start") }
                                    TextButton(onClick = { vm.setVisitStatus(visit, "CANCELLED") }) { Text("Cancel") }
                                    TextButton(onClick = { vm.setVisitStatus(visit, "NO_SHOW") }) { Text("No show") }
                                }
                                if (visit.status == "IN_PROGRESS") TextButton(onClick = { vm.setVisitStatus(visit, "COMPLETED") }) { Text("Complete") }
                            }
                        }
                    }
                }
            }
            "Services" -> {
                Text("Current guide prices (GBP)", style = MaterialTheme.typography.titleLarge)
                listOf("Full groom with bath · Short hair" to "from £40", "Full groom with bath · Long hair / Maine Coon" to "from £45", "Water-free full groom" to "from £40", "De-shedding" to "£30", "Dematting" to "from £30", "Nail trim" to "£6", "Flea surcharge" to "+£5", "Mobile travel" to "Quoted separately").forEach { (name, price) ->
                    Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(name, Modifier.weight(1f)); Text(price) } }
                }
                Text("Reference prices only. Booking price is saved independently.", color = MaterialTheme.colorScheme.secondary)
            }
            "Reports" -> {
                Text("Completed visit reports", style = MaterialTheme.typography.titleLarge)
                Text("Open Visits to share a report PDF. Check private notes before sharing.")
                visits.filter { it.status == "COMPLETED" }.forEach { visit ->
                    val reportCat = cats.firstOrNull { it.id == visit.catId }
                    Text("${reportCat?.name ?: "Unknown"} · ${dateTime(visit.startMillis)}")
                }
            }
            "Notes" -> {
                Text("Visit notes", style = MaterialTheme.typography.titleLarge)
                visits.filter { it.notes.isNotBlank() }.forEach { visit ->
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("${cats.firstOrNull { it.id == visit.catId }?.name ?: "Unknown cat"} · ${dateTime(visit.startMillis)}"); Text(visit.notes); TextButton(onClick={editingVisit=visit}){Text("Edit")} } }
                }
            }
            else -> Text("Planned module. No live functionality yet.")
        }
    }
    bookingError?.let { message ->
        AlertDialog(onDismissRequest = { vm.clearBookingError() }, title = { Text("Booking conflict") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { vm.clearBookingError() }) { Text("OK") } })
    }
    assessingVisit?.let { visit ->
        GroomingAssessmentDialog(assessments.firstOrNull { it.visitId == visit.id } ?: GroomingAssessment(id = java.util.UUID.randomUUID().toString(), visitId = visit.id), onClose = { assessingVisit = null }, onSave = { vm.saveAssessment(it); assessingVisit = null })
    }
    editingVisit?.let { visit ->
        VisitNotesDialog(visit.notes, onClose = { editingVisit = null }, onSave = { notes -> vm.saveVisitNotes(visit, notes); editingVisit = null })
    }
    if (showAdd) when(page) {
        "Clients" -> AddClientDialog(onClose = { showAdd = false }, onSave = { n,p,e,a -> vm.addClient(n,p,e,a); showAdd = false })
        "Cats" -> AddCatDialog(clients, onClose = { showAdd = false }, onSave = { c,n,b,notes -> vm.addCat(c,n,b,notes); showAdd = false })
        "Visits", "Calendar" -> AddVisitDialog(cats, onClose = { showAdd = false }, onSave = { c,t,s,l,p -> vm.addVisit(c,t,s,l,p); showAdd = false })
    }
}
private fun dateTime(ms: Long) = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.UK).withZone(ZoneId.of("Europe/London")).format(java.time.Instant.ofEpochMilli(ms))

@Composable
private fun Field(value: String, onChange: (String)->Unit, label: String) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth())
}
@Composable
private fun AddClientDialog(onClose: ()->Unit, onSave: (String,String,String,String)->Unit) {
    var name by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }; var address by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onClose, title = { Text("New client") }, text = {
        Column { Field(name,{name=it},"Name *"); Field(phone,{phone=it},"Phone"); Field(email,{email=it},"Email"); Field(address,{address=it},"Address") }
    }, confirmButton = { TextButton(enabled=name.isNotBlank(),onClick={onSave(name,phone,email,address)}){Text("Save")} }, dismissButton={TextButton(onClick=onClose){Text("Cancel")}})
}
@Composable
private fun AddCatDialog(clients: List<Client>, onClose: ()->Unit, onSave: (String,String,String,String)->Unit) {
    var owner by remember { mutableStateOf(clients.first().id) }; var expanded by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }; var breed by remember { mutableStateOf("") }; var notes by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest=onClose,title={Text("New cat")},text={
        Column {
            Box { OutlinedButton(onClick={expanded=true}) { Text(clients.firstOrNull { it.id==owner }?.name ?: "Select owner") }
                DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) { clients.forEach { c -> DropdownMenuItem(text={Text(c.name)},onClick={owner=c.id;expanded=false}) } }
            }
            Field(name,{name=it},"Cat name *"); Field(breed,{breed=it},"Breed"); Field(notes,{notes=it},"Handling notes")
        }
    },confirmButton={TextButton(enabled=name.isNotBlank(),onClick={onSave(owner,name,breed,notes)}){Text("Save")}},dismissButton={TextButton(onClick=onClose){Text("Cancel")}})
}
@Composable
private fun AddVisitDialog(cats: List<Cat>, onClose: ()->Unit, onSave: (String,Long,String,String,Long)->Unit) {
    var catId by remember { mutableStateOf(cats.first().id) }; var expanded by remember { mutableStateOf(false) }
    var service by remember { mutableStateOf("Full groom") }; var location by remember { mutableStateOf("Salon") }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd",Locale.UK).format(Date())) }
    var time by remember { mutableStateOf("10:30") }; var price by remember { mutableStateOf("45.00") }
    val parsed = remember(date,time) { runCatching { LocalDateTime.parse("$date $time", DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(java.time.format.ResolverStyle.STRICT)).atZone(ZoneId.of("Europe/London")).toInstant().toEpochMilli() }.getOrNull() }
    val pence = remember(price) { price.toBigDecimalOrNull()?.multiply(java.math.BigDecimal(100))?.toLongExactOrNull() }
    AlertDialog(onDismissRequest=onClose,title={Text("New appointment")},text={
        Column {
            Box { OutlinedButton(onClick={expanded=true}){Text(cats.firstOrNull{it.id==catId}?.name ?: "Select cat")}
                DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) { cats.forEach { c -> DropdownMenuItem(text={Text(c.name)},onClick={catId=c.id;expanded=false}) } }
            }
            Field(date,{date=it},"Date YYYY-MM-DD"); Field(time,{time=it},"Time HH:mm")
            Field(service,{service=it},"Service"); Field(location,{location=it},"Salon or mobile"); Field(price,{price=it},"Price GBP")
        }
    },confirmButton={TextButton(enabled=parsed!=null && pence!=null && pence>=0 && service.isNotBlank(),onClick={onSave(catId,parsed!!,service,location,pence!!)}){Text("Save")}},dismissButton={TextButton(onClick=onClose){Text("Cancel")}})
}
private fun java.math.BigDecimal.toLongExactOrNull(): Long? = runCatching { longValueExact() }.getOrNull()

@Composable
private fun VisitNotesDialog(initial: String, onClose: () -> Unit, onSave: (String) -> Unit) {
    var notes by remember(initial) { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onClose, title = { Text("Grooming notes") }, text = {
        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Observations and handling notes") }, minLines = 4, modifier = Modifier.fillMaxWidth())
    }, confirmButton = { TextButton(onClick = { onSave(notes) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } })
}

@Composable
private fun GroomingAssessmentDialog(
    original: GroomingAssessment,
    onClose: () -> Unit,
    onSave: (GroomingAssessment) -> Unit
) {
    var draft by remember(original.id) { mutableStateOf(original) }
    val tasks = listOf("Brushing", "Bathing", "Dryer", "Nail trim", "Paw handling", "Belly", "Tail")
    val scores = listOf(draft.brushing, draft.bathing, draft.drying, draft.nailTrim, draft.paws, draft.belly, draft.tail)
    fun setScore(index: Int, score: Int) {
        draft = when (index) {
            0 -> draft.copy(brushing = score)
            1 -> draft.copy(bathing = score)
            2 -> draft.copy(drying = score)
            3 -> draft.copy(nailTrim = score)
            4 -> draft.copy(paws = score)
            5 -> draft.copy(belly = score)
            else -> draft.copy(tail = score)
        }
    }
    AlertDialog(onDismissRequest = onClose, title = { Text("Groomer assessment") }, text = {
        Column(Modifier.heightIn(max = 510.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Rate only observed activities. Not assessed is the default.")
            tasks.forEachIndexed { index, label ->
                Text(label, style = MaterialTheme.typography.labelLarge)
                var expanded by remember { mutableStateOf(false) }
                val options = listOf("Not assessed", "Very calm", "Calm", "Okay", "Nervous", "Stressed")
                Box {
                    OutlinedButton(onClick = { expanded = true }) { Text(options[scores[index] + 1]) }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        options.forEachIndexed { optionIndex, option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = { setScore(index, optionIndex - 1); expanded = false })
                        }
                    }
                }
            }
            OutlinedTextField(draft.coatCondition, { draft = draft.copy(coatCondition = it) }, label = { Text("Coat and skin observations") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(draft.recommendations, { draft = draft.copy(recommendations = it) }, label = { Text("Home care recommendations") }, modifier = Modifier.fillMaxWidth())
        }
    }, confirmButton = { TextButton(onClick = { onSave(draft) }) { Text("Save assessment") } },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } })
}

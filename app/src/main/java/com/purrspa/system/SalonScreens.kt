package com.purrspa.system

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
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
    val consentEvents by vm.consentEvents.collectAsStateWithLifecycle()
    val ownerIntakes by vm.ownerIntakes.collectAsStateWithLifecycle()
    var showAdd by rememberSaveable(page) { mutableStateOf(false) }
    var editingClient by remember { mutableStateOf<Client?>(null) }
    var clientHistory by remember { mutableStateOf<Client?>(null) }
    var editingCat by remember { mutableStateOf<Cat?>(null) }
    var catHistory by remember { mutableStateOf<Cat?>(null) }
    var catConsent by remember { mutableStateOf<Cat?>(null) }
    var intakeCat by remember { mutableStateOf<Cat?>(null) }
    var taggingCat by remember { mutableStateOf<Cat?>(null) }
    var editingVisit by remember { mutableStateOf<Visit?>(null) }
    var editingPayment by remember { mutableStateOf<Visit?>(null) }
    var editingCharges by remember { mutableStateOf<Visit?>(null) }
    var pendingStatusChange by remember { mutableStateOf<Pair<Visit, String>?>(null) }
    var reportToShare by remember { mutableStateOf<Visit?>(null) }
    var includePrivateNotes by remember { mutableStateOf(false) }
    var assessingVisit by remember { mutableStateOf<Visit?>(null) }
    var clientSearch by rememberSaveable { mutableStateOf("") }
    var catSearch by rememberSaveable { mutableStateOf("") }
    var visitSearch by rememberSaveable { mutableStateOf("") }
    var visitStatusFilter by rememberSaveable { mutableStateOf("ALL") }
    var calendarDay by rememberSaveable { mutableStateOf(java.time.LocalDate.now(ZoneId.of("Europe/London")).toString()) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(page, style = MaterialTheme.typography.headlineLarge)
        when(page) {
            "Home" -> {
                Text("Clients: ${clients.size}    Cats: ${cats.size}    Visits: ${visits.size}")
                val today = java.time.LocalDate.now(ZoneId.of("Europe/London"))
                val todayVisits = visits.filter { java.time.Instant.ofEpochMilli(it.startMillis).atZone(ZoneId.of("Europe/London")).toLocalDate() == today && it.status !in setOf("CANCELLED", "NO_SHOW") }
                Text("Today: ${todayVisits.size} appointments", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Image(painter = painterResource(R.drawable.purr_cat_charcoal), contentDescription = "Illustrated charcoal cat", modifier = Modifier.size(72.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Upcoming appointments", style = MaterialTheme.typography.titleLarge)
                }
                val upcoming = visits.filter { it.startMillis >= System.currentTimeMillis() && it.status == "SCHEDULED" }.sortedBy { it.startMillis }.take(8)
                if (upcoming.isEmpty()) Text("No upcoming appointments.")
                upcoming.forEach {
                    Text("${cats.firstOrNull { c -> c.id == it.catId }?.name ?: "Unknown cat"} • ${it.service} • ${dateTime(it.startMillis)}")
                }
            }
            "Clients" -> {
                Button(onClick = { showAdd = true }) { Text("Add client") }
                OutlinedTextField(value = clientSearch, onValueChange = { clientSearch = it }, label = { Text("Search clients") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                val matchingClients = clients.filter { clientSearch.isBlank() || it.name.contains(clientSearch, ignoreCase = true) || it.phone.contains(clientSearch, ignoreCase = true) || it.email.contains(clientSearch, ignoreCase = true) || it.address.contains(clientSearch, ignoreCase = true) || cats.any { cat -> cat.clientId == it.id && cat.name.contains(clientSearch, ignoreCase = true) } }
                Text("Showing ${matchingClients.size} of ${clients.size} clients", color = MaterialTheme.colorScheme.secondary)
                if (matchingClients.isEmpty()) Text("No clients match your search.")
                matchingClients.forEach { client ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(client.name, style = MaterialTheme.typography.titleMedium)
                            if (client.phone.isNotBlank()) Text(client.phone, color = MaterialTheme.colorScheme.secondary)
                            Text("${cats.count { it.clientId == client.id }} cats", color = MaterialTheme.colorScheme.secondary)
                        }
                        TextButton(onClick = { clientHistory = client }) { Text("History") }
                        TextButton(onClick = { editingClient = client }) { Text("Edit") }
                    }
                }
            }
            "Cats" -> {
                Button(onClick = { showAdd = true }, enabled = clients.isNotEmpty()) { Text("Add cat") }
                OutlinedTextField(value = catSearch, onValueChange = { catSearch = it }, label = { Text("Search cats") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                val matchingCats = cats.filter { catSearch.isBlank() || it.name.contains(catSearch, ignoreCase = true) || it.breed.contains(catSearch, ignoreCase = true) || clients.firstOrNull { owner -> owner.id == it.clientId }?.let { owner -> owner.name.contains(catSearch, ignoreCase = true) || owner.phone.contains(catSearch, ignoreCase = true) } == true }
                Text("Showing ${matchingCats.size} of ${cats.size} cats", color = MaterialTheme.colorScheme.secondary)
                if (matchingCats.isEmpty()) Text("No cats match your search.")
                matchingCats.forEach { cat ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(cat.name, style = MaterialTheme.typography.titleMedium)
                            Text("${cat.breed.ifBlank { "Breed not specified" }} • Owner: ${clients.firstOrNull { it.id == cat.clientId }?.name ?: "Unknown"}", color = MaterialTheme.colorScheme.secondary)
                            if (cat.sex.isNotBlank() || cat.dateOfBirth.isNotBlank()) Text(listOf(cat.sex, cat.dateOfBirth).filter { it.isNotBlank() }.joinToString(" • "), color = MaterialTheme.colorScheme.secondary)
                            val ownerIntake = ownerIntakes.firstOrNull { it.catId == cat.id }
                            val catVisits = visits.filter { it.catId == cat.id }.sortedByDescending { it.startMillis }
                            val recentAssessment = catVisits.firstNotNullOfOrNull { visit -> assessments.firstOrNull { it.visitId == visit.id } }
                            val flags = buildList {
                                val combined = listOf(cat.notes, cat.healthNotes, ownerIntake?.healthConditions.orEmpty(), ownerIntake?.behaviourTriggers.orEmpty(), recentAssessment?.coatCondition.orEmpty()).joinToString(" ").lowercase()
                                if ("aggress" in combined || "spicy" in combined || "bite" in combined) add("Spicy")
                                if ("senior" in combined || "elderly" in combined) add("Senior")
                                if ("flea" in combined) add("Fleas")
                                if ("mat" in combined || "knot" in combined) add("Mats")
                                if (ownerIntake?.handlingAdvice?.contains("belly", ignoreCase = true) == true || recentAssessment?.belly?.let { it >= 3 } == true) add("Sensitive belly")
                            }
                            val visibleFlags = (cat.handlingTags.split("|").filter { it.isNotBlank() } + flags).distinct()
                            if (visibleFlags.isNotEmpty()) Text(visibleFlags.joinToString("  •  "), color = MaterialTheme.colorScheme.error)
                            val nextAdvice = recentAssessment?.recommendations?.takeIf { it.isNotBlank() } ?: ownerIntake?.handlingAdvice?.takeIf { it.isNotBlank() }
                            if (nextAdvice != null) Text("Next visit: $nextAdvice", color = MaterialTheme.colorScheme.secondary)

                        }
                        TextButton(onClick = { taggingCat = cat }) { Text("Tags") }
                        TextButton(onClick = { intakeCat = cat }) { Text("Owner intake") }
                        TextButton(onClick = { catHistory = cat }) { Text("History") }
                        TextButton(onClick = { editingCat = cat }) { Text("Edit") }
                        Text("Photos: ${if (cat.photoConsent) "Allowed" else "Not allowed"} • Social: ${if (cat.socialConsent) "Allowed" else "Not allowed"}", color = MaterialTheme.colorScheme.secondary)
                        TextButton(onClick = { catConsent = cat }) { Text("Photo consent") }
                    }
                }
            }
            "Visits", "Calendar" -> {
                Button(onClick = { showAdd = true }, enabled = cats.isNotEmpty()) { Text("New visit") }
                OutlinedTextField(value = visitSearch, onValueChange = { visitSearch = it }, label = { Text("Search visits") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                val statuses = listOf("ALL", "SCHEDULED", "IN_PROGRESS", "COMPLETED", "CANCELLED", "NO_SHOW")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    statuses.forEach { status ->
                        FilterChip(
                            selected = visitStatusFilter == status,
                            onClick = { visitStatusFilter = status },
                            label = { Text(status.replace("_", " ")) }
                        )
                    }
                }
                val selectedDay = if (page == "Calendar") runCatching { java.time.LocalDate.parse(calendarDay) }.getOrNull() else null
                if (page == "Calendar") {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        OutlinedTextField(value = calendarDay, onValueChange = { calendarDay = it }, label = { Text("Day YYYY-MM-DD") }, singleLine = true, modifier = Modifier.weight(1f))
                        TextButton(onClick = { calendarDay = java.time.LocalDate.now(ZoneId.of("Europe/London")).toString() }) { Text("Today") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(enabled = selectedDay != null, onClick = { selectedDay?.let { calendarDay = it.minusDays(1).toString() } }) { Text("Previous day") }
                        TextButton(enabled = selectedDay != null, onClick = { selectedDay?.let { calendarDay = it.plusDays(1).toString() } }) { Text("Next day") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(enabled = selectedDay != null, onClick = { selectedDay?.let { calendarDay = it.minusWeeks(1).toString() } }) { Text("Previous week") }
                        TextButton(enabled = selectedDay != null, onClick = { selectedDay?.let { calendarDay = it.plusWeeks(1).toString() } }) { Text("Next week") }
                    }
                    if (selectedDay != null) {
                        val weekStart = selectedDay.with(java.time.DayOfWeek.MONDAY)
                        Text("Week at a glance", style = MaterialTheme.typography.titleMedium)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (0L..6L).forEach { offset ->
                                val day = weekStart.plusDays(offset)
                                val count = visits.count { visit ->
                                    visit.status !in setOf("CANCELLED", "NO_SHOW") &&
                                        java.time.Instant.ofEpochMilli(visit.startMillis).atZone(ZoneId.of("Europe/London")).toLocalDate() == day
                                }
                                FilterChip(
                                    selected = selectedDay == day,
                                    onClick = { calendarDay = day.toString() },
                                    label = { Text("${day.format(DateTimeFormatter.ofPattern("EEE dd", Locale.UK))} · $count") }
                                )
                            }
                        }
                    }
                    if (selectedDay == null) Text("Enter a valid date.", color = MaterialTheme.colorScheme.error)
                }
                val visibleVisits = visits.filter { visit ->
                    val cat = cats.firstOrNull { it.id == visit.catId }
                    val owner = clients.firstOrNull { it.id == cat?.clientId }
                    (page != "Calendar" || (selectedDay != null && java.time.Instant.ofEpochMilli(visit.startMillis).atZone(ZoneId.of("Europe/London")).toLocalDate() == selectedDay)) &&
                    (visitStatusFilter == "ALL" || visit.status == visitStatusFilter) &&
                        (visitSearch.isBlank() ||
                            visit.service.contains(visitSearch, ignoreCase = true) ||
                            cat?.name?.contains(visitSearch, ignoreCase = true) == true ||
                            owner?.name?.contains(visitSearch, ignoreCase = true) == true ||
                            owner?.phone?.contains(visitSearch, ignoreCase = true) == true ||
                            visit.location.contains(visitSearch, ignoreCase = true))
                }.let { filtered ->
                    if (page == "Calendar") filtered.sortedBy { it.startMillis }
                    else filtered.sortedByDescending { it.startMillis }
                }
                if (page == "Calendar" && selectedDay != null) {
                    val activeCount = visits.count { visit ->
                        java.time.Instant.ofEpochMilli(visit.startMillis).atZone(ZoneId.of("Europe/London")).toLocalDate() == selectedDay &&
                            visit.status != "CANCELLED" && visit.status != "NO_SHOW"
                    }
                    Text("${selectedDay.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale.UK))} • $activeCount active appointments", style = MaterialTheme.typography.titleMedium)
                }
                if (page == "Calendar" && selectedDay != null) {
                    val dayBookings = visits.filter {
                        java.time.Instant.ofEpochMilli(it.startMillis).atZone(ZoneId.of("Europe/London")).toLocalDate() == selectedDay
                    }
                    val completedCount = dayBookings.count { it.status == "COMPLETED" }
                    val cancelledCount = dayBookings.count { it.status == "CANCELLED" }
                    val noShowCount = dayBookings.count { it.status == "NO_SHOW" }
                    val bookedPence = dayBookings.filter { it.status !in setOf("CANCELLED", "NO_SHOW") }.sumOf { it.pricePence }
                    Text("Completed: $completedCount • Cancelled: $cancelledCount • No-show: $noShowCount")
                    Text("Active service value: £${"%.2f".format(Locale.UK, bookedPence / 100.0)}", color = MaterialTheme.colorScheme.secondary)
                }
                val scopedTotal = if (page == "Calendar" && selectedDay != null) visits.count { java.time.Instant.ofEpochMilli(it.startMillis).atZone(ZoneId.of("Europe/London")).toLocalDate() == selectedDay } else visits.size
                Text("Showing ${visibleVisits.size} of $scopedTotal visits", color = MaterialTheme.colorScheme.secondary)
                if (visitSearch.isNotBlank() || visitStatusFilter != "ALL") {
                    TextButton(onClick = { visitSearch = ""; visitStatusFilter = "ALL" }) { Text("Clear filters") }
                }
                if (visibleVisits.isEmpty()) {
                    Text(
                        if (page == "Calendar" && selectedDay == null) "Enter a valid date to view appointments."
                        else if (scopedTotal == 0) "No appointments recorded for this ${if (page == "Calendar") "day" else "salon"}."
                        else "No visits match the current search or status filter."
                    )
                }
                visibleVisits.forEach { visit ->
                    Card {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text("${cats.firstOrNull { it.id == visit.catId }?.name ?: "Unknown"} • ${visit.service}", style = MaterialTheme.typography.titleMedium)
                            Text("${dateTime(visit.startMillis)} • ${visit.location} • £${"%.2f".format(Locale.UK, visit.pricePence / 100.0)} • ${visit.status}")
                            val visitOwner = clients.firstOrNull { owner -> owner.id == cats.firstOrNull { it.id == visit.catId }?.clientId }
                            if (visitOwner != null) {
                                Text("Owner: ${visitOwner.name}${if (visitOwner.phone.isNotBlank()) " • ${visitOwner.phone}" else ""}", color = MaterialTheme.colorScheme.secondary)
                            }
                            if (visit.notes.isNotBlank()) Text("Private notes saved • Open Edit notes to view", color = MaterialTheme.colorScheme.secondary)
                            Text("Payment: ${visit.paymentStatus.replace("_", " ")}${if (visit.paymentMethod.isNotBlank()) " • ${visit.paymentMethod.replace("_", " ")}" else ""}")
                            if (visit.travelFeePence > 0 || visit.depositPaidPence > 0) Text("Travel: £${"%.2f".format(Locale.UK, visit.travelFeePence / 100.0)} • Deposit received: £${"%.2f".format(Locale.UK, visit.depositPaidPence / 100.0)}", color = MaterialTheme.colorScheme.secondary)
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { editingVisit = visit }) { Text("Edit notes") }
                                TextButton(onClick = { editingPayment = visit }) { Text("Payment") }
                                TextButton(onClick = { editingCharges = visit }) { Text("Fees / deposit") }
                                TextButton(onClick = { assessingVisit = visit }) { Text("Groomer form") }
                                val reportCat = cats.firstOrNull { it.id == visit.catId }
                                val reportOwner = clients.firstOrNull { it.id == reportCat?.clientId }
                                TextButton(enabled = reportCat != null && reportOwner != null, onClick = {
                                    if (reportCat != null && reportOwner != null) {
                                        includePrivateNotes = false
                                        reportToShare = visit
                                    }
                                }) { Text("Share PDF") }
                                if (visit.status == "SCHEDULED") {
                                    TextButton(onClick = { vm.setVisitStatus(visit, "IN_PROGRESS") }) { Text("Start") }
                                    TextButton(onClick = { pendingStatusChange = visit to "CANCELLED" }) { Text("Cancel") }
                                    TextButton(onClick = { pendingStatusChange = visit to "NO_SHOW" }) { Text("No show") }
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
                val ukZone = ZoneId.of("Europe/London")
                var reportMonth by rememberSaveable { mutableStateOf(java.time.YearMonth.now(ukZone).toString()) }
                val selectedMonth = runCatching { java.time.YearMonth.parse(reportMonth) }.getOrNull()
                Text("Monthly service value", style = MaterialTheme.typography.titleLarge)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = reportMonth,
                        onValueChange = { reportMonth = it },
                        label = { Text("Month YYYY-MM") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { reportMonth = java.time.YearMonth.now(ukZone).toString() }) { Text("This month") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(enabled = selectedMonth != null, onClick = { selectedMonth?.let { reportMonth = it.minusMonths(1).toString() } }) { Text("Previous") }
                    TextButton(enabled = selectedMonth != null, onClick = { selectedMonth?.let { reportMonth = it.plusMonths(1).toString() } }) { Text("Next") }
                }
                if (selectedMonth == null) {
                    Text("Enter a valid month.", color = MaterialTheme.colorScheme.error)
                } else {
                    val monthlyCompleted = visits.filter {
                        it.status == "COMPLETED" &&
                            java.time.YearMonth.from(java.time.Instant.ofEpochMilli(it.startMillis).atZone(ukZone)) == selectedMonth
                    }
                    val serviceValuePence = monthlyCompleted.sumOf { it.pricePence }
                    Text("Completed visits: ${monthlyCompleted.size}", style = MaterialTheme.typography.titleMedium)
                    Text("Service value: £${"%.2f".format(Locale.UK, serviceValuePence / 100.0)}", style = MaterialTheme.typography.titleLarge)
                    Text("Based on completed appointments, not confirmed payments or accounting revenue.", color = MaterialTheme.colorScheme.secondary)
                    val paidCompleted = monthlyCompleted.filter { it.paymentStatus == "PAID" }
                    val paidTotalPence = paidCompleted.sumOf { it.pricePence + it.travelFeePence }
                    Text("Marked fully paid: ${paidCompleted.size} visits • £${"%.2f".format(Locale.UK, paidTotalPence / 100.0)}")
                    val unpaidCompleted = monthlyCompleted.filter { it.paymentStatus != "PAID" }
                    val unpaidBalancePence = unpaidCompleted.sumOf { com.purrspa.system.data.VisitBalance.outstandingPence(it) ?: 0L }
                    Text("Outstanding on completed visits: £${"%.2f".format(Locale.UK, unpaidBalancePence / 100.0)}")
                    Text("Deposits are excluded from outstanding balances; paid flags and deposits are manually recorded.", color = MaterialTheme.colorScheme.secondary)
                    Text("Payment status is manually recorded; amounts are not bank-verified.", color = MaterialTheme.colorScheme.secondary)
                }
                HorizontalDivider()
                Text("Completed visit reports", style = MaterialTheme.typography.titleLarge)
                Text("Review the report contents before sharing.")
                visits.filter { it.status == "COMPLETED" }.forEach { visit ->
                    val reportCat = cats.firstOrNull { it.id == visit.catId }
                    val reportOwner = clients.firstOrNull { it.id == reportCat?.clientId }
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(reportCat?.name ?: "Unknown cat", style = MaterialTheme.typography.titleMedium)
                                Text(dateTime(visit.startMillis))
                            }
                            TextButton(
                                enabled = reportCat != null && reportOwner != null,
                                onClick = { includePrivateNotes = false; reportToShare = visit }
                            ) { Text("Share PDF") }
                        }
                    }
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
    reportToShare?.let { visit ->
        val reportCat = cats.firstOrNull { it.id == visit.catId }
        val reportOwner = clients.firstOrNull { it.id == reportCat?.clientId }
        AlertDialog(
            onDismissRequest = { reportToShare = null },
            title = { Text("Share grooming report?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("The PDF includes the owner's name, cat details, handling scores and home care recommendations.")
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(
                            checked = includePrivateNotes,
                            onCheckedChange = { includePrivateNotes = it }
                        )
                        Text("Include private groomer visit notes")
                    }
                    Text("Confirm the recipient before sending.")
                }
            },
            confirmButton = {
                TextButton(enabled = reportCat != null && reportOwner != null, onClick = {
                    reportToShare = null
                    if (reportCat != null && reportOwner != null) {
                        runCatching { VisitPdf.share(context, if (includePrivateNotes) visit else visit.copy(notes = ""), reportCat, reportOwner, assessments.firstOrNull { it.visitId == visit.id }) }
                            .onFailure { Toast.makeText(context, "Could not share report", Toast.LENGTH_LONG).show() }
                    }
                }) { Text("Continue to share") }
            },
            dismissButton = { TextButton(onClick = { reportToShare = null }) { Text("Cancel") } }
        )
    }
    editingClient?.let { item ->
        EditClientDialog(item, onClose = { editingClient = null }, onSave = { vm.updateClient(it); editingClient = null })
    }
    clientHistory?.let { client ->
        val ownedCats = cats.filter { it.clientId == client.id }
        val catNames = ownedCats.associateBy { it.id }
        val history = visits.filter { it.catId in catNames }.sortedByDescending { it.startMillis }
        val completed = history.filter { it.status == "COMPLETED" }
        AlertDialog(
            onDismissRequest = { clientHistory = null },
            title = { Text("${client.name} • Client history") },
            text = {
                Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${ownedCats.size} cats • ${history.size} visits")
                    Text("${completed.size} completed • Service value: £${"%.2f".format(Locale.UK, completed.sumOf { it.pricePence } / 100.0)}")
                    if (history.isEmpty()) Text("No visits recorded for this client.")
                    history.forEach { visit ->
                        HorizontalDivider()
                        Text("${catNames[visit.catId]?.name ?: "Unknown cat"} • ${dateTime(visit.startMillis)}", style = MaterialTheme.typography.titleSmall)
                        Text("${visit.service} • ${visit.status}")
                        Text("£${"%.2f".format(Locale.UK, visit.pricePence / 100.0)}")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { clientHistory = null }) { Text("Close") } }
        )
    }
    taggingCat?.let { cat ->
        val availableTags = listOf("Spicy", "Senior", "Fleas", "Mats", "Sensitive belly")
        var selected by remember(cat.id) { mutableStateOf(cat.handlingTags.split("|").filter { it.isNotBlank() }.toSet()) }
        AlertDialog(
            onDismissRequest = { taggingCat = null },
            title = { Text("Quick tags • ${cat.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Choose the handling warnings to show on this cat's profile.")
                    availableTags.forEach { tag ->
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Checkbox(checked = tag in selected, onCheckedChange = { checked ->
                                selected = if (checked) selected + tag else selected - tag
                            })
                            Text(tag)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = {
                vm.updateCatHandlingTags(cat.id, selected)
                taggingCat = null
            }) { Text("Save tags") } },
            dismissButton = { TextButton(onClick = { taggingCat = null }) { Text("Cancel") } }
        )
    }
    intakeCat?.let { cat ->
        val saved = ownerIntakes.firstOrNull { it.catId == cat.id }
        var health by rememberSaveable(cat.id) { mutableStateOf(saved?.healthConditions.orEmpty()) }
        var meds by rememberSaveable(cat.id) { mutableStateOf(saved?.medications.orEmpty()) }
        var allergies by rememberSaveable(cat.id) { mutableStateOf(saved?.allergies.orEmpty()) }
        var previous by rememberSaveable(cat.id) { mutableStateOf(saved?.previousGrooming.orEmpty()) }
        var triggers by rememberSaveable(cat.id) { mutableStateOf(saved?.behaviourTriggers.orEmpty()) }
        var advice by rememberSaveable(cat.id) { mutableStateOf(saved?.handlingAdvice.orEmpty()) }
        var brushing by rememberSaveable(cat.id) { mutableStateOf(saved?.brushingTolerance ?: "UNKNOWN") }
        var bathing by rememberSaveable(cat.id) { mutableStateOf(saved?.bathingTolerance ?: "UNKNOWN") }
        var dryer by rememberSaveable(cat.id) { mutableStateOf(saved?.dryerTolerance ?: "UNKNOWN") }
        var nails by rememberSaveable(cat.id) { mutableStateOf(saved?.nailsTolerance ?: "UNKNOWN") }
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Owner intake • ${cat.name}") },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Owner-reported information. Groomer observations are recorded separately.")
                    Text("Draft fields stay on screen until saved. Save intake before leaving this form.", color = MaterialTheme.colorScheme.secondary)
                    OutlinedTextField(health, { health = it }, label = { Text("Health conditions") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(meds, { meds = it }, label = { Text("Medications") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(allergies, { allergies = it }, label = { Text("Allergies / sensitivities") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(previous, { previous = it }, label = { Text("Previous grooming") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(triggers, { triggers = it }, label = { Text("Behaviour and triggers") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(advice, { advice = it }, label = { Text("Handling advice") }, modifier = Modifier.fillMaxWidth())
                    listOf("Brushing" to brushing, "Bathing" to bathing, "Dryer" to dryer, "Nails" to nails).forEach { (label, value) ->
                        Text(label, style = MaterialTheme.typography.titleSmall)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("UNKNOWN", "OK", "SENSITIVE", "DIFFICULT").forEach { choice ->
                                FilterChip(selected = value == choice, onClick = {
                                    when(label) {
                                        "Brushing" -> brushing = choice
                                        "Bathing" -> bathing = choice
                                        "Dryer" -> dryer = choice
                                        else -> nails = choice
                                    }
                                }, label = { Text(choice.lowercase().replaceFirstChar { it.uppercase() }) })
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = {
                vm.saveOwnerIntake(OwnerIntake(
                    id = saved?.id ?: java.util.UUID.randomUUID().toString(), catId = cat.id,
                    healthConditions = health.trim(), medications = meds.trim(), allergies = allergies.trim(),
                    previousGrooming = previous.trim(), behaviourTriggers = triggers.trim(), handlingAdvice = advice.trim(),
                    brushingTolerance = brushing, bathingTolerance = bathing, dryerTolerance = dryer, nailsTolerance = nails
                ))
                intakeCat = null
            }) { Text("Save intake") } },
            dismissButton = { TextButton(onClick = { intakeCat = null }) { Text("Cancel") } }
        )
    }
    catConsent?.let { cat ->
        var photoAllowed by remember(cat.id) { mutableStateOf(cat.photoConsent) }
        var socialAllowed by remember(cat.id) { mutableStateOf(cat.socialConsent) }
        var consentSource by remember(cat.id) { mutableStateOf("STAFF_RECORDED") }
        AlertDialog(
            onDismissRequest = { catConsent = null },
            title = { Text("Photo permissions: ${cat.name}") },
            text = {
                Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Record the owner's explicit permission. Both options default to no.")
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = photoAllowed, onCheckedChange = {
                            photoAllowed = it
                            if (!it) socialAllowed = false
                        })
                        Text("Permission to take photos")
                    }
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = socialAllowed, enabled = photoAllowed, onCheckedChange = { socialAllowed = it })
                        Text("Permission to publish on social media")
                    }
                    Text("How was the decision recorded?")
                    listOf("STAFF_RECORDED", "OWNER_VERBAL", "OWNER_WRITTEN").forEach { option ->
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            RadioButton(selected = consentSource == option, onClick = { consentSource = option })
                            Text(option.replace("_", " "))
                        }
                    }
                    Text("Uncheck either permission to withdraw it. Confirm the owner's choice before saving.")
                    Text("Recorded decisions: ${ConsentHistory.forCat(consentEvents, cat.id).size}")
                    ConsentHistory.forCat(consentEvents, cat.id).forEach { event ->
                        Text("${java.time.Instant.ofEpochMilli(event.recordedMillis).atZone(java.time.ZoneId.of("Europe/London")).format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.UK))} • ${event.source.replace("_", " ")} • Photos: ${if (event.photoAllowed) "Yes" else "No"} • Social: ${if (event.socialAllowed) "Yes" else "No"}")
                    }
                    if (cat.consentUpdatedMillis > 0L) Text("Last recorded: ${java.time.Instant.ofEpochMilli(cat.consentUpdatedMillis).atZone(java.time.ZoneId.of("Europe/London")).format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.UK))}")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.updateCatConsents(cat, photoAllowed, socialAllowed, consentSource)
                    catConsent = null
                }) { Text("Save permissions") }
            },
            dismissButton = { TextButton(onClick = { catConsent = null }) { Text("Cancel") } }
        )
    }
    editingCat?.let { item ->
        EditCatDialog(item, onClose = { editingCat = null }, onSave = { vm.updateCat(it); editingCat = null })
    }
    catHistory?.let { cat ->
        val history = visits.filter { it.catId == cat.id }.sortedByDescending { it.startMillis }
        AlertDialog(
            onDismissRequest = { catHistory = null },
            title = { Text("${cat.name} • Visit history") },
            text = {
                Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Owner: ${clients.firstOrNull { it.id == cat.clientId }?.name ?: "Unknown"}")
                    if (cat.notes.isNotBlank()) Text("Handling notes: ${cat.notes}")
                    clients.firstOrNull { it.id == cat.clientId }?.let { owner -> if (owner.phone.isNotBlank()) Text("Owner phone: ${owner.phone}") }
                    if (cat.sex.isNotBlank()) Text("Sex: ${cat.sex}")
                    if (cat.dateOfBirth.isNotBlank()) Text("Date of birth: ${cat.dateOfBirth}")
                    Text("Neutered: ${if (cat.neutered) "Yes" else "No / not confirmed"}")
                    if (cat.healthNotes.isNotBlank()) Text("Owner-reported health / medication notes: ${cat.healthNotes}")
                    if (cat.healthNotes.isNotBlank()) Text("Private salon information. Check with the owner before sharing.", color = MaterialTheme.colorScheme.secondary)
                    Text("${history.size} recorded visits", style = MaterialTheme.typography.titleMedium)
                    val completed = history.filter { it.status == "COMPLETED" }
                    Text("${completed.size} completed • Service value: £${"%.2f".format(Locale.UK, completed.sumOf { it.pricePence } / 100.0)}")
                    if (history.isEmpty()) Text("No visits recorded for this cat.")
                    history.forEach { visit ->
                        HorizontalDivider()
                        Text(dateTime(visit.startMillis), style = MaterialTheme.typography.titleSmall)
                        Text("${visit.service} • ${visit.status}")
                        Text("£${"%.2f".format(Locale.UK, visit.pricePence / 100.0)} • ${visit.location}")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { catHistory = null }) { Text("Close") } }
        )
    }
    bookingError?.let { message ->
        AlertDialog(onDismissRequest = { vm.clearBookingError() }, title = { Text("Booking conflict") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { vm.clearBookingError() }) { Text("OK") } })
    }
    assessingVisit?.let { visit ->
        GroomingAssessmentDialog(assessments.firstOrNull { it.visitId == visit.id } ?: GroomingAssessment(id = java.util.UUID.randomUUID().toString(), visitId = visit.id), onClose = { assessingVisit = null }, onSave = { vm.saveAssessment(it); assessingVisit = null })
    }
    pendingStatusChange?.let { (visit, status) ->
        AlertDialog(
            onDismissRequest = { pendingStatusChange = null },
            title = { Text(if (status == "CANCELLED") "Cancel appointment?" else "Mark as no-show?") },
            text = {
                Text("Confirm change for ${cats.firstOrNull { it.id == visit.catId }?.name ?: "Unknown cat"} on ${dateTime(visit.startMillis)}. The booking slot will become available.")
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setVisitStatus(visit, status)
                    pendingStatusChange = null
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { pendingStatusChange = null }) { Text("Keep booking") }
            }
        )
    }
    editingCharges?.let { visit ->
        var travel by remember(visit.id) { mutableStateOf("%.2f".format(Locale.UK, visit.travelFeePence / 100.0)) }
        var deposit by remember(visit.id) { mutableStateOf("%.2f".format(Locale.UK, visit.depositPaidPence / 100.0)) }
        val travelPence = remember(travel) { travel.trim().toBigDecimalOrNull()?.takeIf { it.scale() <= 2 }?.multiply(java.math.BigDecimal(100))?.let { runCatching { it.longValueExact() }.getOrNull() } }
        val depositPence = remember(deposit) { deposit.trim().toBigDecimalOrNull()?.takeIf { it.scale() <= 2 }?.multiply(java.math.BigDecimal(100))?.let { runCatching { it.longValueExact() }.getOrNull() } }
        val total = travelPence?.let { runCatching { Math.addExact(visit.pricePence, it) }.getOrNull() }
        val valid = travelPence != null && depositPence != null && com.purrspa.system.data.VisitBalance.validCharges(visit.pricePence, travelPence, depositPence)
        AlertDialog(
            onDismissRequest = { editingCharges = null },
            title = { Text("Travel fee and deposit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Service: £${"%.2f".format(Locale.UK, visit.pricePence / 100.0)}")
                    Field(travel, { travel = it }, "Travel fee GBP")
                    Field(deposit, { deposit = it }, "Deposit already received GBP")
                    if (total != null && depositPence != null && valid) Text("Remaining: £${"%.2f".format(Locale.UK, (total - depositPence) / 100.0)}")
                    if (!valid) Text("Enter valid amounts. Deposit cannot exceed the total.", color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = { TextButton(enabled = valid, onClick = {
                vm.setVisitCharges(visit, travelPence!!, depositPence!!)
                editingCharges = null
            }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editingCharges = null }) { Text("Cancel") } }
        )
    }
    editingPayment?.let { visit ->
        var method by remember(visit.id) { mutableStateOf(visit.paymentMethod.ifBlank { "CASH" }) }
        AlertDialog(
            onDismissRequest = { editingPayment = null },
            title = { Text("Visit payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val fullAmount = visit.pricePence + visit.travelFeePence
                    Text("Total including travel: £${"%.2f".format(Locale.UK, fullAmount / 100.0)}")
                    Text("Deposit recorded: £${"%.2f".format(Locale.UK, visit.depositPaidPence / 100.0)}")
                    Text("Balance before final payment: £${"%.2f".format(Locale.UK, (fullAmount - visit.depositPaidPence) / 100.0)}")
                    Text("Record whether this visit has been paid in full.")
                    if (visit.status in setOf("CANCELLED", "NO_SHOW")) Text("Cancelled and no-show visits cannot be marked paid in this version.", color = MaterialTheme.colorScheme.error)
                    listOf("CASH", "CARD", "BANK_TRANSFER", "OTHER").forEach { option ->
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            RadioButton(selected = method == option, onClick = { method = option })
                            Text(option.replace("_", " "))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = visit.status !in setOf("CANCELLED", "NO_SHOW"), onClick = { vm.setVisitPayment(visit, "PAID", method); editingPayment = null }) { Text("Mark paid") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { vm.setVisitPayment(visit, "UNPAID", ""); editingPayment = null }) { Text("Mark unpaid") }
                    TextButton(onClick = { editingPayment = null }) { Text("Close") }
                }
            }
        )
    }
    editingVisit?.let { visit ->
        VisitNotesDialog(visit.notes, onClose = { editingVisit = null }, onSave = { notes -> vm.saveVisitNotes(visit, notes); editingVisit = null })
    }
    if (showAdd) when(page) {
        "Clients" -> AddClientDialog(onClose = { showAdd = false }, onSave = { n,p,e,a -> vm.addClient(n,p,e,a); showAdd = false })
        "Cats" -> AddCatDialog(clients, onClose = { showAdd = false }, onSave = { c,n,b,notes,sex,dob,neutered,health -> vm.addCat(c,n,b,notes,sex,dob,neutered,health); showAdd = false })
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
    var name by rememberSaveable { mutableStateOf("") }; var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }; var address by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = onClose, title = { Text("New client") }, text = {
        Column(Modifier.heightIn(max = 470.dp).verticalScroll(rememberScrollState())) { Field(name,{name=it},"Name *"); Field(phone,{phone=it},"Phone"); Field(email,{email=it},"Email"); Field(address,{address=it},"Address") }
    }, confirmButton = { TextButton(enabled=name.isNotBlank(),onClick={onSave(name,phone,email,address)}){Text("Save")} }, dismissButton={TextButton(onClick=onClose){Text("Cancel")}})
}
@Composable
private fun AddCatDialog(clients: List<Client>, onClose: ()->Unit, onSave: (String,String,String,String,String,String,Boolean,String)->Unit) {
    var owner by rememberSaveable { mutableStateOf(clients.first().id) }; var expanded by remember { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }; var breed by rememberSaveable { mutableStateOf("") }; var notes by rememberSaveable { mutableStateOf("") }
    var sex by rememberSaveable { mutableStateOf("") }; var dob by rememberSaveable { mutableStateOf("") }
    var neutered by rememberSaveable { mutableStateOf(false) }; var health by rememberSaveable { mutableStateOf("") }
    val validDob = dob.isBlank() || runCatching { java.time.LocalDate.parse(dob) }.getOrNull()?.let { !it.isAfter(java.time.LocalDate.now(ZoneId.of("Europe/London"))) } == true
    AlertDialog(onDismissRequest=onClose,title={Text("New cat")},text={
        Column(Modifier.heightIn(max = 470.dp).verticalScroll(rememberScrollState())) {
            Box { OutlinedButton(onClick={expanded=true}) { Text(clients.firstOrNull { it.id==owner }?.name ?: "Select owner") }
                DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) { clients.forEach { c -> DropdownMenuItem(text={Text(c.name)},onClick={owner=c.id;expanded=false}) } }
            }
            Field(name,{name=it},"Cat name *"); Field(breed,{breed=it},"Breed")
            Field(sex,{sex=it},"Sex (optional)"); Field(dob,{dob=it},"Date of birth YYYY-MM-DD (optional)")
            if (!validDob) Text("Enter a valid birth date, not in the future.", color = MaterialTheme.colorScheme.error)
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { Checkbox(checked=neutered,onCheckedChange={neutered=it}); Text("Neutered") }
            OutlinedTextField(health,{health=it},label={Text("Owner-reported health and medication notes")},modifier=Modifier.fillMaxWidth())
            Field(notes,{notes=it},"Handling notes")
        }
    },confirmButton={TextButton(enabled=name.isNotBlank() && validDob,onClick={onSave(owner,name,breed,notes,sex,dob,neutered,health)}){Text("Save")}},dismissButton={TextButton(onClick=onClose){Text("Cancel")}})
}
@Composable
private fun AddVisitDialog(cats: List<Cat>, onClose: ()->Unit, onSave: (String,Long,String,String,Long)->Unit) {
    var catId by rememberSaveable { mutableStateOf(cats.first().id) }; var expanded by remember { mutableStateOf(false) }
    var service by rememberSaveable { mutableStateOf("Full groom") }; var location by rememberSaveable { mutableStateOf("Salon") }
    var date by rememberSaveable { mutableStateOf(java.time.LocalDate.now(ZoneId.of("Europe/London")).toString()) }
    var time by rememberSaveable { mutableStateOf("10:30") }; var price by rememberSaveable { mutableStateOf("45.00") }
    val parsed = remember(date,time) { runCatching { LocalDateTime.parse("$date $time", DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(java.time.format.ResolverStyle.STRICT)).let { local -> ZoneId.of("Europe/London").rules.getValidOffsets(local).singleOrNull()?.let { offset -> local.toInstant(offset).toEpochMilli() } } }.getOrNull() }
    val pence = remember(price) { price.trim().toBigDecimalOrNull()?.takeIf { it.scale() <= 2 }?.multiply(java.math.BigDecimal(100))?.let { runCatching { it.longValueExact() }.getOrNull() } }
    val futureDate = parsed != null && AppointmentRules.validNewStart(parsed, System.currentTimeMillis())
    AlertDialog(onDismissRequest=onClose,title={Text("New appointment")},text={
        Column(Modifier.heightIn(max = 470.dp).verticalScroll(rememberScrollState())) {
            Box { OutlinedButton(onClick={expanded=true}){Text(cats.firstOrNull{it.id==catId}?.name ?: "Select cat")}
                DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) { cats.forEach { c -> DropdownMenuItem(text={Text(c.name)},onClick={catId=c.id;expanded=false}) } }
            }
            Field(date,{date=it},"Date YYYY-MM-DD"); Field(time,{time=it},"Time HH:mm")
            if (parsed == null) Text("Enter a valid UK date and time. Clock-change times may be unavailable or ambiguous.", color = MaterialTheme.colorScheme.error)
            else if (!futureDate) Text("Choose a future appointment time.", color = MaterialTheme.colorScheme.error)
            Field(service,{service=it},"Service"); Field(location,{location=it},"Salon or mobile"); Field(price,{price=it},"Price GBP")
            if (pence == null || pence < 0) Text("Enter a valid GBP amount (0 or more, up to 2 decimal places).", color = MaterialTheme.colorScheme.error)
        }
    },confirmButton={TextButton(enabled=futureDate && pence!=null && pence>=0 && service.isNotBlank(),onClick={onSave(catId,parsed!!,service,location,pence!!)}){Text("Save")}},dismissButton={TextButton(onClick=onClose){Text("Cancel")}})
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

@Composable
private fun EditClientDialog(item: Client, onClose: () -> Unit, onSave: (Client) -> Unit) {
    var name by remember(item.id) { mutableStateOf(item.name) }
    var phone by remember(item.id) { mutableStateOf(item.phone) }
    var email by remember(item.id) { mutableStateOf(item.email) }
    var address by remember(item.id) { mutableStateOf(item.address) }
    AlertDialog(onDismissRequest = onClose, title = { Text("Edit client") }, text = {
        Column(Modifier.heightIn(max = 470.dp).verticalScroll(rememberScrollState())) {
            Field(name, { name = it }, "Name")
            Field(phone, { phone = it }, "Phone")
            Field(email, { email = it }, "Email")
            Field(address, { address = it }, "Address")
        }
    }, confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = {
        onSave(item.copy(name = name.trim(), phone = phone.trim(), email = email.trim(), address = address.trim()))
    }) { Text("Save") } }, dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } })
}

@Composable
private fun EditCatDialog(item: Cat, onClose: () -> Unit, onSave: (Cat) -> Unit) {
    var name by remember(item.id) { mutableStateOf(item.name) }
    var breed by remember(item.id) { mutableStateOf(item.breed) }
    var notes by remember(item.id) { mutableStateOf(item.notes) }
    var sex by remember(item.id) { mutableStateOf(item.sex) }
    var dob by remember(item.id) { mutableStateOf(item.dateOfBirth) }
    var neutered by remember(item.id) { mutableStateOf(item.neutered) }
    var healthNotes by remember(item.id) { mutableStateOf(item.healthNotes) }
    val validDob = dob.isBlank() || runCatching { java.time.LocalDate.parse(dob) }.getOrNull()?.let { !it.isAfter(java.time.LocalDate.now(ZoneId.of("Europe/London"))) } == true
    AlertDialog(onDismissRequest = onClose, title = { Text("Edit cat") }, text = {
        Column(Modifier.heightIn(max = 470.dp).verticalScroll(rememberScrollState())) {
            Field(name, { name = it }, "Name")
            Field(breed, { breed = it }, "Breed")
            Field(sex, { sex = it }, "Sex (optional)")
            Field(dob, { dob = it }, "Date of birth YYYY-MM-DD (optional)")
            if (!validDob) Text("Enter a valid birth date, not in the future.", color = MaterialTheme.colorScheme.error)
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = neutered, onCheckedChange = { neutered = it })
                Text("Neutered")
            }
            OutlinedTextField(healthNotes, { healthNotes = it }, label = { Text("Owner-reported health and medication notes") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(notes, { notes = it }, label = { Text("Handling notes") }, modifier = Modifier.fillMaxWidth())
        }
    }, confirmButton = { TextButton(enabled = name.isNotBlank() && validDob, onClick = {
        onSave(item.copy(name = name.trim(), breed = breed.trim(), notes = notes.trim(), sex = sex.trim(), dateOfBirth = dob.trim(), neutered = neutered, healthNotes = healthNotes.trim()))
    }) { Text("Save") } }, dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } })
}

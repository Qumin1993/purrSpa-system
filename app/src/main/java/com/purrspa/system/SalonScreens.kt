package com.purrspa.system

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.purrspa.system.data.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SalonScreen(page: String, vm: SalonViewModel) {
    val clients by vm.clients.collectAsState()
    val cats by vm.cats.collectAsState()
    val visits by vm.visits.collectAsState()
    var showAdd by remember(page) { mutableStateOf(false) }
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
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            else -> Text("Planned module. No live functionality yet.")
        }
    }
    if (showAdd) when(page) {
        "Clients" -> AddClientDialog(onClose = { showAdd = false }, onSave = { n,p,e,a -> vm.addClient(n,p,e,a); showAdd = false })
        "Cats" -> AddCatDialog(clients, onClose = { showAdd = false }, onSave = { c,n,b,notes -> vm.addCat(c,n,b,notes); showAdd = false })
        "Visits", "Calendar" -> AddVisitDialog(cats, onClose = { showAdd = false }, onSave = { c,t,s,l,p -> vm.addVisit(c,t,s,l,p); showAdd = false })
    }
}
private fun dateTime(ms: Long) = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.UK).format(Date(ms))

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
    val parsed = remember(date,time) { runCatching { SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.UK).apply { isLenient=false }.parse("$date $time")?.time }.getOrNull() }
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

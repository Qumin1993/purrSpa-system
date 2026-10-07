package com.purrspa.system

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF141310)
private val Gold = Color(0xFFD9BA7D)
private val Rose = Color(0xFFCA5977)
private val Cream = Color(0xFFFFFAF3)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PurrSpaApp() }
    }
}

private val pages = listOf("Home", "Clients", "Cats", "Visits", "Calendar", "Notes", "Services", "Reports", "Settings")
private val symbols = listOf("⌂", "♙", "♧", "▤", "▦", "✎", "✂", "▧", "⚙")

@Composable
private fun PurrSpaApp() {
    var page by rememberSaveable { mutableStateOf("Home") }
    MaterialTheme(colorScheme = lightColorScheme(primary = Rose, background = Cream, surface = Color.White)) {
        Row(Modifier.fillMaxSize().background(Cream)) {
            Column(
                Modifier.width(208.dp).fillMaxHeight().background(Ink).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("♧", color = Gold, fontSize = 35.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                Text("Purr Spa", color = Gold, fontSize = 29.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                Text("C A T  G R O O M I N G", color = Gold, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(20.dp))
                pages.forEachIndexed { index, name ->
                    val selected = page == name
                    Surface(
                        onClick = { page = name },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) Rose else Color.Transparent
                    ) {
                        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(symbols[index], color = if (selected) Color.White else Gold, fontSize = 22.sp)
                            Spacer(Modifier.width(14.dp))
                            Text(name, color = Color.White)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("Happy Cats\nHappier People ♥", color = Gold, lineHeight = 24.sp)
            }
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(28.dp)) {
                Text(if (page == "Home") "Welcome to Purr Spa" else page, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.height(8.dp))
                Text(if (page == "Home") "Your grooming studio at a glance" else "Module implementation planned. No customer data stored yet.", color = Color.Gray)
                Spacer(Modifier.height(24.dp))
                if (page == "Home") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("Today's visits", "Clients", "Cats", "Pending reports").forEach {
                            Surface(Modifier.weight(1f), shape = RoundedCornerShape(16.dp), color = Color.White, tonalElevation = 2.dp) {
                                Column(Modifier.padding(20.dp)) {
                                    Text("0", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Rose)
                                    Text(it, fontSize = 13.sp, color = Ink)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Surface(shape = RoundedCornerShape(18.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(24.dp)) {
                            Text("Today's schedule", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            Text("No appointments yet. Visit management will be added in the next phase.", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

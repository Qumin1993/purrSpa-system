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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.purrspa.system.data.SalonViewModel
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
    val salon: SalonViewModel = viewModel()
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
                SalonScreen(page, salon)
            }
        }
    }
}

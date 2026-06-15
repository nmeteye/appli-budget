package com.nicolas.budgetcouple

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nicolas.budgetcouple.data.*
import com.nicolas.budgetcouple.ui.StatTile
import com.nicolas.budgetcouple.ui.eur
import com.nicolas.budgetcouple.ui.screens.*
import com.nicolas.budgetcouple.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            BudgetCoupleTheme {
                Surface(color = Cream) { AppRoot() }
            }
        }
    }
}

@Composable
fun AppRoot() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val vm: BudgetViewModel = viewModel(factory = BudgetViewModel.Factory(context))
    val state by vm.state.collectAsState()
    val t = state.totals()
    var tab by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = Cream,
        bottomBar = {
            NavigationBar(containerColor = Paper) {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.People, null) },
                    label = { Text("Budget") }
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.Lightbulb, null) },
                    label = { Text("Économies") }
                )
                NavigationBarItem(
                    selected = tab == 2, onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.TrackChanges, null) },
                    label = { Text("Placements") }
                )
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // En-tête
            Spacer(Modifier.height(20.dp))
            Text("LIVRE DE COMPTES — FOYER", fontSize = 11.sp, color = Sage,
                fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
            Text("Le grand livre du couple", fontSize = 30.sp, fontWeight = FontWeight.Normal,
                color = Ink, lineHeight = 36.sp)
            Text("Trois comptes, un foyer. Suivez, économisez, placez.",
                fontSize = 14.sp, color = Muted)
            Spacer(Modifier.height(16.dp))

            // Bandeau résumé
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Solde total", eur(t.balance), Ink, Modifier.weight(1f))
                StatTile("Revenus", eur(t.income), Sage, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Dépenses", eur(t.expense), Brique, Modifier.weight(1f))
                StatTile("Épargne possible", eur(t.net), Ocre, Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))

            when (tab) {
                0 -> BudgetScreen(state, vm)
                1 -> SaveScreen(state, vm)
                2 -> InvestScreen(state)
            }

            Spacer(Modifier.height(28.dp))
            Text(
                "Outil d'organisation personnelle. Les pistes de placement sont informatives " +
                    "et ne constituent pas un conseil financier individualisé.",
                fontSize = 12.sp, color = Muted, lineHeight = 17.sp
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

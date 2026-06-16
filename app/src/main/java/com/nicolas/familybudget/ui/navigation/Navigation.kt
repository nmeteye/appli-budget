package com.nicolas.familybudget.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nicolas.familybudget.ui.accounts.AccountsScreen
import com.nicolas.familybudget.ui.advice.AdviceScreen
import com.nicolas.familybudget.ui.dashboard.DashboardScreen
import com.nicolas.familybudget.ui.invest.InvestScreen
import com.nicolas.familybudget.ui.settings.SettingsScreen
import com.nicolas.familybudget.ui.transactions.TransactionsScreen

sealed class Dest(val route: String, val label: String, val icon: ImageVector) {
    data object Dashboard : Dest("dashboard", "Tableau", Icons.Filled.Dashboard)
    data object Accounts : Dest("accounts", "Comptes", Icons.Filled.AccountBalanceWallet)
    data object Advice : Dest("advice", "Conseils", Icons.Filled.Lightbulb)
    data object Invest : Dest("invest", "Placement", Icons.Filled.TrendingUp)
    data object Settings : Dest("settings", "Reglages", Icons.Filled.Settings)
}

private val bottomItems = listOf(Dest.Dashboard, Dest.Accounts, Dest.Advice, Dest.Invest, Dest.Settings)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomItems.forEach { item ->
                    val selected = currentDest?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Dest.Dashboard.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Dest.Dashboard.route) {
                DashboardScreen(onOpenAccounts = { navController.navigate(Dest.Accounts.route) })
            }
            composable(Dest.Accounts.route) {
                AccountsScreen(onOpenAccount = { id ->
                    navController.navigate("transactions/$id")
                })
            }
            composable(
                route = "transactions/{accountId}",
                arguments = listOf(navArgument("accountId") { type = NavType.LongType }),
            ) { entry ->
                val accountId = entry.arguments?.getLong("accountId") ?: 0L
                TransactionsScreen(accountId = accountId, onBack = { navController.popBackStack() })
            }
            composable(Dest.Advice.route) { AdviceScreen() }
            composable(Dest.Invest.route) { InvestScreen() }
            composable(Dest.Settings.route) { SettingsScreen() }
        }
    }
}

package info.meuse24.smsforwarderneoA1.presentation.ui.components.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import info.meuse24.smsforwarderneoA1.ContactsViewModel

@Composable
fun BottomNavigationBar(navController: NavController, viewModel: ContactsViewModel) {
    val mailScreenVisible by viewModel.mailScreenVisible.collectAsState()
    val items = if (mailScreenVisible) {
        listOf("start", "mail", "setup", "log", "info")
    } else {
        listOf("start", "setup", "log", "info")
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Navigate away from mail screen if it becomes hidden
    LaunchedEffect(mailScreenVisible, currentRoute) {
        if (!mailScreenVisible && currentRoute == "mail") {
            navController.navigate("start") {
                popUpTo("start") { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavigationBar(
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    when (screen) {
                        "setup" -> Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Setup"
                        )

                        "mail" -> Icon(
                            Icons.Filled.Email,
                            contentDescription = "Mail"
                        )

                        "log" -> Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = "Log"
                        )

                        "info" -> Icon(
                            Icons.Filled.Info,
                            contentDescription = "Info"
                        )

                        else -> Icon(
                            Icons.Filled.Home,
                            contentDescription = "Start"
                        )
                    }
                },
                label = {
                    Text(
                        when (screen) {
                            "start" -> "Start"
                            "mail" -> "Mail"
                            "setup" -> "Setup"
                            "log" -> "Log"
                            else -> "Info"
                        }
                    )
                },
                selected = currentRoute == screen,
                onClick = {
                    if (screen == "start") {
                        // Für Start: BackStack komplett clearen und HomeScreen anzeigen
                        navController.navigate(screen) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        // Für andere Screens: Standard-Navigation mit State-Speicherung
                        navController.navigate(screen) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                        }
                    }
                }
            )
        }
    }
}

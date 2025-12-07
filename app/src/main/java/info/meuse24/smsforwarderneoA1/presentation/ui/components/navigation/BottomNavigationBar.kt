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

@Composable
fun BottomNavigationBar(
    screens: List<String>,
    currentPage: Int,
    onPageSelected: (Int) -> Unit
) {
    NavigationBar(
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        screens.forEachIndexed { index, screen ->
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
                selected = currentPage == index,
                onClick = { onPageSelected(index) }
            )
        }
    }
}

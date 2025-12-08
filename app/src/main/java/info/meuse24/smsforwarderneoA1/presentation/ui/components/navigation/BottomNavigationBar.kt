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
import androidx.compose.ui.res.stringResource
import info.meuse24.smsforwarderneoA1.R

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
                            contentDescription = stringResource(R.string.tab_setup)
                        )

                        "mail" -> Icon(
                            Icons.Filled.Email,
                            contentDescription = stringResource(R.string.tab_mail)
                        )

                        "log" -> Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = stringResource(R.string.tab_log)
                        )

                        "info" -> Icon(
                            Icons.Filled.Info,
                            contentDescription = stringResource(R.string.tab_info)
                        )

                        else -> Icon(
                            Icons.Filled.Home,
                            contentDescription = stringResource(R.string.tab_home)
                        )
                    }
                },
                label = {
                    Text(
                        when (screen) {
                            "start" -> stringResource(R.string.tab_home)
                            "mail" -> stringResource(R.string.tab_mail)
                            "setup" -> stringResource(R.string.tab_setup)
                            "log" -> stringResource(R.string.tab_log)
                            else -> stringResource(R.string.tab_info)
                        }
                    )
                },
                selected = currentPage == index,
                onClick = { onPageSelected(index) }
            )
        }
    }
}

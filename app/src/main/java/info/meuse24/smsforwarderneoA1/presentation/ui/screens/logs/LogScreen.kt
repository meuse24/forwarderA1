package info.meuse24.smsforwarderneoA1.presentation.ui.screens.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.LogViewModel
import info.meuse24.smsforwarderneoA1.ui.theme.BackgroundGradientLight

/**
 * Main Log Screen showing application logs with filtering and sharing capabilities.
 *
 * Displays logs in a responsive layout:
 * - Landscape: Side-by-side table and buttons
 * - Portrait: Stacked table over buttons
 *
 * @param logViewModel The LogViewModel instance for log data and operations
 */
@Composable
fun LogScreen(logViewModel: LogViewModel) {
    val context = LocalContext.current
    val logEntriesHtml by logViewModel.logEntriesHtml.collectAsState()
    val showAllLogs by logViewModel.showAllLogs.collectAsState()

    LaunchedEffect(Unit) {
        logViewModel.reloadLogs()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGradientLight)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            @Suppress("UNUSED_EXPRESSION")
            val isLandscape = this.maxWidth > this.maxHeight

            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Log Table Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        LogTable(logViewModel)
                    }

                    // Button Column
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FilterLogButton(logViewModel, showAllLogs)
                            ShareLogIconButton(context, logEntriesHtml)
                            RefreshLogButton(logViewModel)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Log Table Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        LogTable(logViewModel)
                    }

                    // Button Row Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterLogButton(logViewModel, showAllLogs)
                            ShareLogIconButton(context, logEntriesHtml)
                            RefreshLogButton(logViewModel)
                        }
                    }
                }
            }
        }
    }
}

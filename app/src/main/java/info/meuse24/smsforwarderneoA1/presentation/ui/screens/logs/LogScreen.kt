package info.meuse24.smsforwarderneoA1.presentation.ui.screens.logs

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.ContactsViewModel

/**
 * Main Log Screen showing application logs with filtering and sharing capabilities.
 *
 * Displays logs in a responsive layout:
 * - Landscape: Side-by-side table and buttons
 * - Portrait: Stacked table over buttons
 *
 * @param viewModel The ContactsViewModel instance for log data and operations
 */
@Composable
fun LogScreen(viewModel: ContactsViewModel) {
    val context = LocalContext.current
    val logEntriesHtml by viewModel.logEntriesHtml.collectAsState()
    val showAllLogs by viewModel.showAllLogs.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.reloadLogs()
    }

    BoxWithConstraints {
        @Suppress("UNUSED_EXPRESSION")
        val isLandscape = this.maxWidth > this.maxHeight

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()

            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    LogTable(viewModel)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Icon-Spalte ohne weight, nur mit der benötigten Breite
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FilterLogButton(viewModel, showAllLogs)
                        ShareLogIconButton(context, logEntriesHtml)
                        RefreshLogButton(viewModel)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()

            ) {
                Box(modifier = Modifier.weight(1f)) {
                    LogTable(viewModel)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterLogButton(viewModel, showAllLogs)
                    Spacer(modifier = Modifier.width(16.dp))
                    ShareLogIconButton(context, logEntriesHtml)
                    Spacer(modifier = Modifier.width(16.dp))
                    RefreshLogButton(viewModel)
                }
            }
        }
    }
}

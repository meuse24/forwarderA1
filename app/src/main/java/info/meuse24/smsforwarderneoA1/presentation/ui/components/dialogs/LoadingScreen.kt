package info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs

import android.os.Build
import androidx.compose.foundation.Image
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.R

/**
 * Loading screen shown during app initialization.
 *
 * Displays the app logo, loading indicator, and initialization status.
 * If an error occurs during initialization, shows an error message with retry option.
 *
 * @param error Optional error message to display. If provided, shows error state with buttons.
 * @param onRetry Callback when user clicks retry button (only shown when error is not null)
 * @param onExit Callback when user clicks exit button (only shown when error is not null)
 */
@Composable
fun LoadingScreen(
    error: String?,
    onRetry: (() -> Unit)? = null,
    onExit: (() -> Unit)? = null
) {
    val hasError = error != null && (onRetry != null || onExit != null)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Logo
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = R.drawable.logofwd2,
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(scaleX = 1.5f, scaleY = 1.5f)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_title_line_1),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.app_title_line_2),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (!hasError) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                Text(
                    text = error ?: stringResource(R.string.msg_initializing_app),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = error ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    // Buttons werden NUR angezeigt, wenn Callbacks vorhanden sind
                    if (onRetry != null || onExit != null) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            onRetry?.let {
                                Button(
                                    onClick = it,
                                    modifier = Modifier.fillMaxWidth(0.8f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    androidx.compose.foundation.layout.Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null
                                        )
                                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.btn_retry))
                                    }
                                }
                            }

                            onExit?.let {
                                Button(
                                    onClick = it,
                                    modifier = Modifier.fillMaxWidth(0.8f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    androidx.compose.foundation.layout.Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null
                                        )
                                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.btn_exit_app))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

package info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Warning
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
 * @param error Optional error message to display. If null, shows loading state.
 * @param onRetry Callback when user clicks retry button (only shown on error)
 * @param onExit Callback when user clicks exit button (only shown on error)
 */
@Composable
fun LoadingScreen(
    error: String?,
    onRetry: (() -> Unit)? = null,
    onExit: (() -> Unit)? = null
) {
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
                Image(
                    painter = painterResource(id = R.drawable.logofwd2),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(scaleX = 1.5f, scaleY = 1.5f)
                )
            }

            Text(
                text = "SMS/TEL Forwarder",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (error == null) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Text(
                        text = "App wird initialisiert...",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Erforderliche Berechtigungen:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        listOf(
                            "Kontakte - Für die Auswahl des Weiterleitungsziels",
                            "SMS - Zum Empfangen und Weiterleiten von Nachrichten",
                            "Telefon - Für die Anrufweiterleitung",
                            "Batterieoptimierung - Für Stabilität im Hintergrund"
                        ).forEach { permission ->
                            androidx.compose.foundation.layout.Row(
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = permission,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(36.dp)
                    )

                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )

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
                                    Text("Neu versuchen")
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
                                    Text("App beenden")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

package info.meuse24.smsforwarderneoA1.presentation.ui.screens.home

import android.telephony.TelephonyManager
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.meuse24.smsforwarderneoA1.ContactsViewModel
import info.meuse24.smsforwarderneoA1.domain.model.Contact
import info.meuse24.smsforwarderneoA1.presentation.ui.components.AnimatedButton
import info.meuse24.smsforwarderneoA1.presentation.ui.components.AnimatedCard
import info.meuse24.smsforwarderneoA1.presentation.ui.components.AnimatedOutlinedButton
import info.meuse24.smsforwarderneoA1.presentation.ui.components.GradientBorderCard
import info.meuse24.smsforwarderneoA1.presentation.ui.components.GradientButton
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.EmailViewModel
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.TestUtilsViewModel
import info.meuse24.smsforwarderneoA1.ui.theme.AnimationHelpers
import info.meuse24.smsforwarderneoA1.ui.theme.BackgroundGradientLight
import info.meuse24.smsforwarderneoA1.ui.theme.ErrorGradient
import info.meuse24.smsforwarderneoA1.ui.theme.PrimaryGradient

@Composable
fun HomeScreen(
    viewModel: ContactsViewModel,
    emailViewModel: EmailViewModel,
    testUtilsViewModel: TestUtilsViewModel,
    callState: androidx.compose.runtime.State<Int>
) {
    val selectedContact by viewModel.selectedContact.collectAsState()
    val forwardingActive by viewModel.forwardingActive.collectAsState()
    val currentCallState by callState

    // Check if call is active (for button disabling)
    val isCallActive = currentCallState == TelephonyManager.CALL_STATE_OFFHOOK

    // Initialisierung beim ersten Laden
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    // Background with subtle gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGradientLight)
    ) {
        BoxWithConstraints {
            @Suppress("UNUSED_EXPRESSION")
            val isLandscape = this.maxWidth > this.maxHeight

            if (isLandscape) {
                LandscapeLayout(
                    viewModel = viewModel,
                    emailViewModel = emailViewModel,
                    testUtilsViewModel = testUtilsViewModel,
                    selectedContact = selectedContact,
                    forwardingActive = forwardingActive,
                    isCallActive = isCallActive,
                    callState = currentCallState
                )
            } else {
                PortraitLayout(
                    viewModel = viewModel,
                    emailViewModel = emailViewModel,
                    testUtilsViewModel = testUtilsViewModel,
                    selectedContact = selectedContact,
                    forwardingActive = forwardingActive,
                    isCallActive = isCallActive,
                    callState = currentCallState
                )
            }
        }
    }
}

@Composable
fun LandscapeLayout(
    viewModel: ContactsViewModel,
    emailViewModel: EmailViewModel,
    testUtilsViewModel: TestUtilsViewModel,
    selectedContact: Contact?,
    forwardingActive: Boolean,
    isCallActive: Boolean,
    callState: Int
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left side: Contact selection
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ContactSelectionSection(
                selectedContact = selectedContact,
                forwardingActive = forwardingActive,
                isCallActive = isCallActive,
                onSelectContact = { viewModel.launchContactPicker() },
                onDeactivate = { viewModel.deactivateCurrentForwarding() },
                onSendTestSms = { testUtilsViewModel.sendTestSms(selectedContact) }
            )
        }

        // Right side: Status and controls
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            CallStatusCard(callState = callState)

            Spacer(modifier = Modifier.weight(1f))

            // Bottom buttons with animations
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status Info Button
                AnimatedOutlinedButton(
                    onClick = { viewModel.queryForwardingStatus() },
                    enabled = !isCallActive,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Status abfragen",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Status abfragen",
                        textAlign = TextAlign.Center
                    )
                }

                // Reset Button with error gradient
                GradientButton(
                    onClick = {
                        viewModel.deactivateCurrentForwarding()
                        emailViewModel.updateForwardSmsToEmail(false)
                        viewModel.queryForwardingStatus()
                    },
                    enabled = !isCallActive,
                    modifier = Modifier.fillMaxWidth(),
                    gradient = ErrorGradient
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Alle Weiterleitungen zurücksetzen",
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun PortraitLayout(
    viewModel: ContactsViewModel,
    emailViewModel: EmailViewModel,
    testUtilsViewModel: TestUtilsViewModel,
    selectedContact: Contact?,
    forwardingActive: Boolean,
    isCallActive: Boolean,
    callState: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Contact selection section
        ContactSelectionSection(
            selectedContact = selectedContact,
            forwardingActive = forwardingActive,
            isCallActive = isCallActive,
            onSelectContact = { viewModel.launchContactPicker() },
            onDeactivate = { viewModel.deactivateCurrentForwarding() },
            onSendTestSms = { testUtilsViewModel.sendTestSms(selectedContact) }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Status section
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CallStatusCard(callState = callState)

            // Status Info Button
            AnimatedOutlinedButton(
                onClick = { viewModel.queryForwardingStatus() },
                enabled = !isCallActive,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Status abfragen",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Status abfragen",
                    textAlign = TextAlign.Center
                )
            }

            // Reset Button with gradient
            GradientButton(
                onClick = {
                    viewModel.deactivateCurrentForwarding()
                    emailViewModel.updateForwardSmsToEmail(false)
                    viewModel.queryForwardingStatus()
                },
                enabled = !isCallActive,
                modifier = Modifier.fillMaxWidth(),
                gradient = ErrorGradient
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Alle Weiterleitungen zurücksetzen",
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Contact selection section with picker button or contact card
 */
@Composable
fun ContactSelectionSection(
    selectedContact: Contact?,
    forwardingActive: Boolean,
    isCallActive: Boolean,
    onSelectContact: () -> Unit,
    onDeactivate: () -> Unit,
    onSendTestSms: () -> Unit
) {
    // Animated visibility for contact card
    val isContactSelected = selectedContact != null && forwardingActive

    if (!isContactSelected) {
        // No contact selected: Show selection button with pulse animation
        val pulseScale by AnimationHelpers.animatePulse(targetValue = 1.05f, initialValue = 1f)

        GradientButton(
            onClick = onSelectContact,
            enabled = !isCallActive,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .scale(if (!isCallActive) pulseScale else 1f),
            gradient = PrimaryGradient
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Kontakt auswählen",
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .size(48.dp)
                )
                Text(
                    text = "Kontakt für Weiterleitung auswählen",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        // Contact selected: Show animated card with gradient border
        AnimatedCard(
            visible = isContactSelected,
            modifier = Modifier.fillMaxWidth(),
            elevation = 8.dp
        ) {
            GradientBorderCard(
                modifier = Modifier.fillMaxWidth(),
                borderWidth = 3.dp,
                backgroundColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    Text(
                        text = "Aktive Weiterleitung",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )

                    // Contact info
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = selectedContact?.name ?: "",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = selectedContact?.phoneNumber ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        if (selectedContact?.description?.isNotEmpty() == true &&
                            selectedContact.description != selectedContact.phoneNumber) {
                            Text(
                                text = selectedContact.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Action buttons - First row with animations
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnimatedOutlinedButton(
                            onClick = onSelectContact,
                            enabled = !isCallActive,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Kontakt ändern",
                                textAlign = TextAlign.Center
                            )
                        }
                        AnimatedOutlinedButton(
                            onClick = onSendTestSms,
                            enabled = !isCallActive,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Test-SMS",
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Deactivate button - Second row with gradient
                    GradientButton(
                        onClick = onDeactivate,
                        enabled = !isCallActive,
                        modifier = Modifier.fillMaxWidth(),
                        gradient = ErrorGradient
                    ) {
                        Text(
                            text = "Deaktivieren",
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

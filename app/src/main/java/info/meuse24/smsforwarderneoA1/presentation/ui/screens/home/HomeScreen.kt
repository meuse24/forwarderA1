package info.meuse24.smsforwarderneoA1.presentation.ui.screens.home

import android.telephony.TelephonyManager
import androidx.compose.animation.core.LinearEasing
import coil.compose.AsyncImage
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.PhoneForwarded
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontFamily
import info.meuse24.smsforwarderneoA1.ContactsViewModel
import info.meuse24.smsforwarderneoA1.R
import info.meuse24.smsforwarderneoA1.domain.model.Contact
import info.meuse24.smsforwarderneoA1.domain.model.ForwardingVerification
import info.meuse24.smsforwarderneoA1.domain.model.MmiOperationState
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
import info.meuse24.smsforwarderneoA1.ui.theme.WarmContactGradient

@Composable
private fun ForwardingVerificationCard(viewModel: ContactsViewModel) {
    val verification by viewModel.forwardingVerification.collectAsState()
    val forwardingActive by viewModel.forwardingActive.collectAsState()
    val showTransientHint by viewModel.showTransientForwardingHint.collectAsState()
    if (verification == ForwardingVerification.NOT_CHECKED ||
        (verification == ForwardingVerification.ASSUMED_SUCCESS && !showTransientHint)
    ) return

    // A1's voice response cannot be parsed. Show the operational hint briefly,
    // but never require an acknowledgement before the user can continue working.
    // It is deliberately not recreated from persisted status after an app restart.
    LaunchedEffect(verification, showTransientHint) {
        if (verification == ForwardingVerification.ASSUMED_SUCCESS && showTransientHint) {
            delay(8_000)
            viewModel.dismissTransientForwardingHint()
        }
    }

    val text = when (verification) {
        ForwardingVerification.ASSUMED_SUCCESS -> stringResource(
            if (forwardingActive) {
                R.string.forwarding_verification_assumed_activate
            } else {
                R.string.forwarding_verification_assumed_deactivate
            }
        )
        ForwardingVerification.CONFIRMED_SUCCESS -> stringResource(R.string.forwarding_verification_confirmed)
        ForwardingVerification.UNKNOWN_NO_RESPONSE -> stringResource(R.string.forwarding_verification_unknown)
        ForwardingVerification.DIAL_FAILED -> stringResource(R.string.forwarding_verification_failed)
        ForwardingVerification.USER_REPORTED_FAILURE -> stringResource(R.string.forwarding_verification_user_failed)
        ForwardingVerification.NOT_CHECKED -> return
    }
    // Der haeufigste Fall ist die transiente Erfolgsmeldung nach dem Wahlvorgang. Sie steht
    // nur 8 Sekunden und wuerde als hohe Karte den Inhalt darunter - insbesondere den
    // Deaktivieren-Button - aus dem sichtbaren Bereich schieben. Deshalb hier kompakt in
    // einer Zeile: Text links, Aktion rechts.
    val compactAssumedSuccess = verification == ForwardingVerification.ASSUMED_SUCCESS && forwardingActive
    if (compactAssumedSuccess) {
        AnimatedCard(visible = true, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = viewModel::reportForwardingFailure) {
                    Text(stringResource(R.string.forwarding_report_failure))
                }
            }
        }
        return
    }

    AnimatedCard(visible = true, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text, fontWeight = FontWeight.Medium)
            if (verification == ForwardingVerification.ASSUMED_SUCCESS) {
                Button(onClick = viewModel::reportForwardingFailure) { Text(stringResource(R.string.forwarding_report_failure)) }
            }
            if (verification == ForwardingVerification.UNKNOWN_NO_RESPONSE ||
                verification == ForwardingVerification.USER_REPORTED_FAILURE ||
                (verification == ForwardingVerification.ASSUMED_SUCCESS && !forwardingActive)
            ) {
                Button(onClick = viewModel::queryForwardingStatus) { Text(stringResource(R.string.forwarding_query_status)) }
            }
            if (verification == ForwardingVerification.DIAL_FAILED || verification == ForwardingVerification.USER_REPORTED_FAILURE) {
                Button(onClick = viewModel::retryLastForwardingOperation) { Text(stringResource(R.string.forwarding_retry)) }
            }
            if (verification == ForwardingVerification.USER_REPORTED_FAILURE) {
                Button(onClick = viewModel::continueWithAssumedForwarding) { Text(stringResource(R.string.forwarding_continue)) }
            }
        }
    }
}

@Composable
private fun PendingForwardingCard(viewModel: ContactsViewModel) {
    val pending by viewModel.pendingForwardingRequest.collectAsState()
    val operation = pending?.takeIf { it.state == MmiOperationState.DIALING } ?: return
    AnimatedCard(visible = true, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(
                if (operation.action == ContactsViewModel.ForwardingAction.ACTIVATE) R.string.forwarding_operation_activating
                else R.string.forwarding_operation_deactivating
            ),
            modifier = Modifier.padding(12.dp),
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Animated app logo with one-time 360° rotation on screen open and on touch
 */
@Composable
fun AnimatedAppLogo(modifier: Modifier = Modifier) {
    // State to track rotation target (increments by 360° on each click)
    var rotationTarget by remember { mutableFloatStateOf(0f) }

    // Start animation on first composition
    LaunchedEffect(Unit) {
        rotationTarget = 360f
    }

    // Rotation animation
    val rotation by animateFloatAsState(
        targetValue = rotationTarget,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "logo_rotation"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = R.drawable.logofwd2,
            contentDescription = stringResource(R.string.desc_app_logo),
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable {
                    rotationTarget += 360f
                }
                .graphicsLayer {
                    rotationZ = rotation
                }
        )
    }
}

@Composable
fun HomeScreen(
    viewModel: ContactsViewModel,
    emailViewModel: EmailViewModel,
    testUtilsViewModel: TestUtilsViewModel,
    callState: androidx.compose.runtime.State<Int>,
    onNavigateToHelp: () -> Unit = {},
    onNavigateToRcsHelp: () -> Unit = onNavigateToHelp
) {
    val selectedContact by viewModel.selectedContact.collectAsState()
    val forwardingActive by viewModel.forwardingActive.collectAsState()
    val pendingOperation by viewModel.pendingForwardingRequest.collectAsState()
    val currentCallState by callState

    // Check if call is active (for button disabling)
    val isCallActive = currentCallState == TelephonyManager.CALL_STATE_OFFHOOK || pendingOperation != null

    // Initialisierung beim ersten Laden
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    // Background with wallpaper image
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Wallpaper background
        AsyncImage(
            model = R.drawable.wallpaper,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        BoxWithConstraints {
            val isLandscape = this.maxWidth > this.maxHeight

            if (isLandscape) {
                LandscapeLayout(
                    viewModel = viewModel,
                    emailViewModel = emailViewModel,
                    testUtilsViewModel = testUtilsViewModel,
                    selectedContact = selectedContact,
                    forwardingActive = forwardingActive,
                    isCallActive = isCallActive,
                    callState = currentCallState,
                    onNavigateToHelp = onNavigateToHelp,
                    onNavigateToRcsHelp = onNavigateToRcsHelp
                )
            } else {
                PortraitLayout(
                    viewModel = viewModel,
                    emailViewModel = emailViewModel,
                    testUtilsViewModel = testUtilsViewModel,
                    selectedContact = selectedContact,
                    forwardingActive = forwardingActive,
                    isCallActive = isCallActive,
                    callState = currentCallState,
                    onNavigateToHelp = onNavigateToHelp,
                    onNavigateToRcsHelp = onNavigateToRcsHelp
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
    callState: Int,
    onNavigateToHelp: () -> Unit = {},
    onNavigateToRcsHelp: () -> Unit = onNavigateToHelp
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top area
        CallStatusCard(callState = callState)
        PendingForwardingCard(viewModel)
        ForwardingVerificationCard(viewModel)
        RcsHintCardHost(onNavigateToHelp = onNavigateToRcsHelp)

        Spacer(modifier = Modifier.weight(1f))

        // Bottom area: Contact selection + Controls in row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Contact selection (left)
            Box(
                modifier = Modifier.weight(1f)
            ) {
                ContactSelectionSection(
                    selectedContact = selectedContact,
                    forwardingActive = forwardingActive,
                    isCallActive = isCallActive,
                    viewModel = viewModel,
                    emailViewModel = emailViewModel,
                    onSelectContact = { viewModel.launchContactPicker() },
                    onDeactivate = { viewModel.deactivateCurrentForwarding() },
                    onSendTestSms = { testUtilsViewModel.sendTestSms(selectedContact) }
                )
            }

            // Button row (right)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Help Button
                FloatingActionButton(
                    onClick = onNavigateToHelp,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                    modifier = Modifier.graphicsLayer { alpha = 0.7f }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Help,
                        contentDescription = stringResource(R.string.btn_show_help),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Status Info Button
                FloatingActionButton(
                    onClick = { if (!isCallActive) viewModel.queryForwardingStatus() },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                    modifier = Modifier.graphicsLayer { alpha = 0.7f }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PhoneForwarded,
                        contentDescription = stringResource(R.string.btn_query_status),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Reset Button
                FloatingActionButton(
                    onClick = {
                        if (!isCallActive) {
                            emailViewModel.updateForwardSmsToEmail(false)
                            viewModel.resetAllForwarding()
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                    modifier = Modifier.graphicsLayer { alpha = 0.7f }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.desc_reset_forwarding),
                        modifier = Modifier.size(28.dp)
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
    callState: Int,
    onNavigateToHelp: () -> Unit = {},
    onNavigateToRcsHelp: () -> Unit = onNavigateToHelp
) {
    rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top area with CallStatusCard
        CallStatusCard(callState = callState)
        PendingForwardingCard(viewModel)
        ForwardingVerificationCard(viewModel)
        RcsHintCardHost(onNavigateToHelp = onNavigateToRcsHelp)

        Spacer(modifier = Modifier.weight(1f))

        // Bottom area: Contact selection + Buttons (rotated container)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { rotationZ = -2f },
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Contact selection (button or card)
            ContactSelectionSection(
                selectedContact = selectedContact,
                forwardingActive = forwardingActive,
                isCallActive = isCallActive,
                viewModel = viewModel,
                emailViewModel = emailViewModel,
                onSelectContact = { viewModel.launchContactPicker() },
                onDeactivate = { viewModel.deactivateCurrentForwarding() },
                onSendTestSms = { testUtilsViewModel.sendTestSms(selectedContact) }
            )

            // Bottom row: Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Help Button
                FloatingActionButton(
                    onClick = onNavigateToHelp,
                    containerColor = Color.Transparent,
                    contentColor = Color.Black,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .border(
                            width = 2.dp,
                            color = Color.Black.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(
                            brush = WarmContactGradient,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Help,
                        contentDescription = stringResource(R.string.btn_show_help),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Status Info Button
                FloatingActionButton(
                    onClick = { if (!isCallActive) viewModel.queryForwardingStatus() },
                    containerColor = Color.Transparent,
                    contentColor = Color.Black,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .border(
                            width = 2.dp,
                            color = Color.Black.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(
                            brush = WarmContactGradient,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PhoneForwarded,
                        contentDescription = stringResource(R.string.btn_query_status),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Reset Button
                FloatingActionButton(
                    onClick = {
                        if (!isCallActive) {
                            emailViewModel.updateForwardSmsToEmail(false)
                            viewModel.resetAllForwarding()
                        }
                    },
                    containerColor = Color.Transparent,
                    contentColor = Color.Black,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .border(
                            width = 2.dp,
                            color = Color.Black.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(
                            brush = WarmContactGradient,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.desc_reset_forwarding),
                        modifier = Modifier.size(28.dp)
                    )
                }
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
    viewModel: ContactsViewModel,
    emailViewModel: EmailViewModel,
    onSelectContact: () -> Unit,
    onDeactivate: () -> Unit,
    onSendTestSms: () -> Unit
) {
    // Collect states for service info
    val mmiSimSelectionMode by viewModel.mmiSimSelectionMode.collectAsState()
    val defaultVoiceSubscriptionId by viewModel.defaultVoiceSubscriptionId.collectAsState()
    val availableSimCards by viewModel.availableSimCards.collectAsState()
    val forwardSmsToEmail by emailViewModel.forwardSmsToEmail.collectAsState()
    val sim1ReceiveEnabled by viewModel.sim1ReceiveEnabled.collectAsState()
    val sim2ReceiveEnabled by viewModel.sim2ReceiveEnabled.collectAsState()
    val simSelectionMode by viewModel.simSelectionMode.collectAsState()
    val defaultSmsSubscriptionId by viewModel.defaultSmsSubscriptionId.collectAsState()

    // Animated visibility for contact card
    val isContactSelected = selectedContact != null && forwardingActive

    if (!isContactSelected) {
        // No contact selected: Show selection button with pulse animation
        val pulseScale by AnimationHelpers.animatePulse(targetValue = 1.05f, initialValue = 1f)

        GradientButton(
            onClick = onSelectContact,
            enabled = !isCallActive,
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(120.dp)
                .scale(if (!isCallActive) pulseScale else 1f),
            gradient = WarmContactGradient
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AsyncImage(
                    model = R.drawable.officer,
                    contentDescription = stringResource(R.string.btn_select_contact),
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .size(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
                Text(
                    text = stringResource(R.string.btn_select_contact),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    color = Color.Black
                )
            }
        }
    } else {
        // Contact selected: Show animated card with gradient border
        AnimatedCard(
            visible = true,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = 0.7f },
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
                    // Header row with title and logo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.heading_active_forwarding).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            letterSpacing = 1.2.sp
                        )
                        AnimatedAppLogo()
                    }

                    // Contact info
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = selectedContact.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = selectedContact.phoneNumber,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    // Service info section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // SIM card info for call forwarding (MMI)
                        val simInfo = when (mmiSimSelectionMode) {
                            info.meuse24.smsforwarderneoA1.domain.model.MmiSimSelectionMode.DEFAULT_VOICE_SIM -> {
                                val defaultSim = availableSimCards.find {
                                    it.subscriptionId == defaultVoiceSubscriptionId
                                }
                                if (defaultSim != null) {
                                    stringResource(
                                        R.string.mmi_sim_default_voice_display,
                                        defaultSim.carrierName?.takeIf { it.isNotBlank() }
                                            ?: stringResource(R.string.mmi_sim_unknown_carrier)
                                    )
                                } else {
                                    stringResource(R.string.badge_default_voice)
                                }
                            }
                            info.meuse24.smsforwarderneoA1.domain.model.MmiSimSelectionMode.ALWAYS_SIM_1 -> {
                                val sim = availableSimCards.getOrNull(0)
                                if (sim != null) {
                                    stringResource(
                                        R.string.mmi_sim_slot_display,
                                        1,
                                        sim.carrierName?.takeIf { it.isNotBlank() }
                                            ?: stringResource(R.string.mmi_sim_unknown_carrier)
                                    )
                                } else {
                                    stringResource(R.string.mmi_sim_slot_unavailable, 1)
                                }
                            }
                            info.meuse24.smsforwarderneoA1.domain.model.MmiSimSelectionMode.ALWAYS_SIM_2 -> {
                                val sim = availableSimCards.getOrNull(1)
                                if (sim != null) {
                                    stringResource(
                                        R.string.mmi_sim_slot_display,
                                        2,
                                        sim.carrierName?.takeIf { it.isNotBlank() }
                                            ?: stringResource(R.string.mmi_sim_unknown_carrier)
                                    )
                                } else {
                                    stringResource(R.string.mmi_sim_slot_unavailable, 2)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PhoneForwarded,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = simInfo,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }

                        // SMS Eingang (Receive Filter info)
                        val smsEingangInfo = buildString {
                            val enabledSims = mutableListOf<String>()
                            if (sim1ReceiveEnabled) {
                                val sim1 = availableSimCards.getOrNull(0)
                                enabledSims.add(if (sim1 != null) "SIM 1 (${sim1.carrierName})" else "SIM 1")
                            }
                            if (sim2ReceiveEnabled) {
                                val sim2 = availableSimCards.getOrNull(1)
                                enabledSims.add(if (sim2 != null) "SIM 2 (${sim2.carrierName})" else "SIM 2")
                            }

                            append("SMS Eingang: ")
                            if (enabledSims.isEmpty()) {
                                append("⚠️ Keine")
                            } else {
                                append(enabledSims.joinToString(", "))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (!sim1ReceiveEnabled && !sim2ReceiveEnabled) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = smsEingangInfo,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = if (!sim1ReceiveEnabled && !sim2ReceiveEnabled) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                }
                            )
                        }

                        // SMS Ausgang (Send SIM Selection)
                        val smsAusgangInfo = buildString {
                            append("SMS Ausgang: ")
                            when (simSelectionMode) {
                                info.meuse24.smsforwarderneoA1.domain.model.SimSelectionMode.SAME_AS_INCOMING -> {
                                    append("Gleiche SIM wie Eingang")
                                }
                                info.meuse24.smsforwarderneoA1.domain.model.SimSelectionMode.ALWAYS_SIM_1 -> {
                                    val sim1 = availableSimCards.getOrNull(0)
                                    if (sim1 != null) {
                                        val isDefault = sim1.subscriptionId == defaultSmsSubscriptionId && defaultSmsSubscriptionId != -1
                                        append("SIM 1 (${sim1.carrierName})")
                                        if (isDefault) append(" - Standard")
                                    } else {
                                        append("SIM 1 (nicht verfügbar)")
                                    }
                                }
                                info.meuse24.smsforwarderneoA1.domain.model.SimSelectionMode.ALWAYS_SIM_2 -> {
                                    val sim2 = availableSimCards.getOrNull(1)
                                    if (sim2 != null) {
                                        val isDefault = sim2.subscriptionId == defaultSmsSubscriptionId && defaultSmsSubscriptionId != -1
                                        append("SIM 2 (${sim2.carrierName})")
                                        if (isDefault) append(" - Standard")
                                    } else {
                                        append("SIM 2 (nicht verfügbar)")
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = smsAusgangInfo,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }

                        // Active services
                        val services = buildList {
                            add("SMS")
                            add("Call")
                            if (forwardSmsToEmail) add("E-Mail")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Dienste: ${services.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
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
                                text = stringResource(R.string.btn_change_contact),
                                textAlign = TextAlign.Center
                            )
                        }
                        AnimatedOutlinedButton(
                            onClick = onSendTestSms,
                            enabled = !isCallActive,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.btn_test_sms),
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
                            text = stringResource(R.string.btn_deactivate),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

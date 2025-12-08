package info.meuse24.smsforwarderneoA1.presentation.ui.screens.home

import android.telephony.TelephonyManager
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import info.meuse24.smsforwarderneoA1.ContactsViewModel
import info.meuse24.smsforwarderneoA1.R
import info.meuse24.smsforwarderneoA1.domain.model.Contact
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
        Image(
            painter = painterResource(id = R.drawable.logofwd2),
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
    onNavigateToHelp: () -> Unit = {}
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

    // Background with wallpaper image
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Wallpaper background
        Image(
            painter = painterResource(id = R.drawable.wallpaper),
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
                    onNavigateToHelp = onNavigateToHelp
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
                    onNavigateToHelp = onNavigateToHelp
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
    onNavigateToHelp: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top area
        CallStatusCard(callState = callState)

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
                    onClick = { viewModel.queryForwardingStatus() },
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
                        emailViewModel.updateForwardSmsToEmail(false)
                        viewModel.resetAllForwarding()
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
    onNavigateToHelp: () -> Unit = {}
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
                    onClick = { viewModel.queryForwardingStatus() },
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
                        emailViewModel.updateForwardSmsToEmail(false)
                        viewModel.resetAllForwarding()
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
                .fillMaxWidth(0.75f)
                .height(120.dp)
                .scale(if (!isCallActive) pulseScale else 1f),
            gradient = WarmContactGradient
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.officer),
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
                            text = stringResource(R.string.heading_active_forwarding),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
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
                        if (selectedContact.description.isNotEmpty() && selectedContact.description != selectedContact.phoneNumber) {
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

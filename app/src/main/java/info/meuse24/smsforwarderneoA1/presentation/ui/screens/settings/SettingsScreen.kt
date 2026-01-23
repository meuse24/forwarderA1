package info.meuse24.smsforwarderneoA1.presentation.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import coil.compose.AsyncImage
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.AppContainer
import info.meuse24.smsforwarderneoA1.ContactsViewModel
import info.meuse24.smsforwarderneoA1.LoggingManager
import info.meuse24.smsforwarderneoA1.R
import info.meuse24.smsforwarderneoA1.SnackbarManager
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.ChangePinDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.PinDialog
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.EmailViewModel
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.NavigationViewModel
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.SimManagementViewModel
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.TestUtilsViewModel

@Composable
fun SettingsScreen(
    viewModel: ContactsViewModel,
    emailViewModel: EmailViewModel,
    testUtilsViewModel: TestUtilsViewModel,
    navigationViewModel: NavigationViewModel,
    simManagementViewModel: SimManagementViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    LocalFocusManager.current
    var isAnyFieldFocused by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    val sectionTitleStyle = MaterialTheme.typography.titleMedium

    // Position tracking for each section
    var simManagementPosition by remember { mutableStateOf(0f) }
    var simSelectionPosition by remember { mutableStateOf(0f) }
    var callForwardingPosition by remember { mutableStateOf(0f) }
    var emailSettingsPosition by remember { mutableStateOf(0f) }
    var appSettingsPosition by remember { mutableStateOf(0f) }
    var logSettingsPosition by remember { mutableStateOf(0f) }

    // Expansion states for each section - alle geschlossen beim Start
    var simManagementExpanded by remember { mutableStateOf(false) }
    var simSelectionExpanded by remember { mutableStateOf(false) }
    var callForwardingExpanded by remember { mutableStateOf(false) }
    var emailSettingsExpanded by remember { mutableStateOf(false) }
    var appSettingsExpanded by remember { mutableStateOf(false) }
    var logSettingsExpanded by remember { mutableStateOf(false) }

    // Accordion-Funktion: Schließt alle anderen Sections
    fun collapseAllExcept(section: String) {
        if (section != "simManagement") simManagementExpanded = false
        if (section != "simSelection") simSelectionExpanded = false
        if (section != "callForwarding") callForwardingExpanded = false
        if (section != "email") emailSettingsExpanded = false
        if (section != "app") appSettingsExpanded = false
        if (section != "log") logSettingsExpanded = false
    }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Section 1: SIM Management (inkl. Telefon-Einstellungen)
            ExpandableSection(
                title = stringResource(R.string.section_sim_management),
                expanded = simManagementExpanded,
                onExpandChange = {
                    if (it) {
                        collapseAllExcept("simManagement")
                        coroutineScope.launch {
                            scrollState.animateScrollTo(simManagementPosition.toInt())
                        }
                    }
                    simManagementExpanded = it
                },
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    simManagementPosition = coordinates.positionInParent().y
                }
            ) {
                SimManagementSection(
                    viewModel = viewModel,
                    simManagementViewModel = simManagementViewModel,
                    onFocusChanged = { isAnyFieldFocused = it },
                    sectionTitleStyle = sectionTitleStyle
                )
            }

            // Section 3: SIM Selection (inkl. SMS-Empfangsfilter)
            ExpandableSection(
                title = stringResource(R.string.section_sms_sim_selection),
                expanded = simSelectionExpanded,
                onExpandChange = {
                    if (it) {
                        collapseAllExcept("simSelection")
                        coroutineScope.launch {
                            scrollState.animateScrollTo(simSelectionPosition.toInt())
                        }
                    }
                    simSelectionExpanded = it
                },
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    simSelectionPosition = coordinates.positionInParent().y
                }
            ) {
                SimSelectionSection(
                    viewModel = viewModel,
                    sectionTitleStyle = sectionTitleStyle
                )
            }

            // Section 4: Email Forwarding
            ExpandableSection(
                title = stringResource(R.string.section_email_settings),
                expanded = emailSettingsExpanded,
                onExpandChange = {
                    if (it) {
                        collapseAllExcept("email")
                        coroutineScope.launch {
                            scrollState.animateScrollTo(emailSettingsPosition.toInt())
                        }
                    }
                    emailSettingsExpanded = it
                },
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    emailSettingsPosition = coordinates.positionInParent().y
                }
            ) {
                EmailSettingsSection(
                    emailViewModel = emailViewModel,
                    sectionTitleStyle = sectionTitleStyle,
                    onMailTabVisibilityChanged = viewModel::updateMailScreenVisibility
                )
            }

            // Section 5: Call Forwarding (MMI SIM + Codes)
            ExpandableSection(
                title = stringResource(R.string.section_call_forwarding_settings),
                expanded = callForwardingExpanded,
                onExpandChange = {
                    if (it) {
                        collapseAllExcept("callForwarding")
                        coroutineScope.launch {
                            scrollState.animateScrollTo(callForwardingPosition.toInt())
                        }
                    }
                    callForwardingExpanded = it
                },
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    callForwardingPosition = coordinates.positionInParent().y
                }
            ) {
                CallForwardingSettingsSection(
                    viewModel = viewModel,
                    sectionTitleStyle = sectionTitleStyle,
                    onFocusChanged = { isAnyFieldFocused = it }
                )
            }

            // Section 6: App Settings
            ExpandableSection(
                title = stringResource(R.string.section_app_settings),
                expanded = appSettingsExpanded,
                onExpandChange = {
                    if (it) {
                        collapseAllExcept("app")
                        coroutineScope.launch {
                            scrollState.animateScrollTo(appSettingsPosition.toInt())
                        }
                    }
                    appSettingsExpanded = it
                },
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    appSettingsPosition = coordinates.positionInParent().y
                }
            ) {
                AppSettingsSection(
                    viewModel = viewModel,
                    emailViewModel = emailViewModel,
                    testUtilsViewModel = testUtilsViewModel,
                    navigationViewModel = navigationViewModel,
                    onFocusChanged = { isAnyFieldFocused = it },
                    sectionTitleStyle = sectionTitleStyle
                )
            }

            // Section 7: Log Settings
            ExpandableSection(
                title = stringResource(R.string.section_logs_security),
                expanded = logSettingsExpanded,
                onExpandChange = {
                    if (it) {
                        collapseAllExcept("log")
                        coroutineScope.launch {
                            scrollState.animateScrollTo(logSettingsPosition.toInt())
                        }
                    }
                    logSettingsExpanded = it
                },
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    logSettingsPosition = coordinates.positionInParent().y
                }
            ) {
                LogSettingsSection(
                    sectionTitleStyle = sectionTitleStyle,
                    onDeleteLogs = { showPinDialog = true },
                    onChangePin = { showChangePinDialog = true },
                    viewModel = viewModel,
                    onFocusChanged = { isAnyFieldFocused = it }
                )
            }
        }

        // PIN Dialoge
        if (showPinDialog) {
            PinDialog(
                storedPin = AppContainer.requirePrefsManager().getLogPIN(),
                onPinCorrect = {
                    LoggingManager.getFileTree().clearLog()
                    LoggingManager.logInfo(
                        component = "SettingsScreen",
                        action = "CLEAR_LOGS",
                        message = "Log-Einträge wurden gelöscht"
                    )
                    SnackbarManager.showSuccess(context.getString(R.string.snackbar_logs_deleted))
                    showPinDialog = false
                },
                onDismiss = { showPinDialog = false }
            )
        }

        if (showChangePinDialog) {
            ChangePinDialog(
                storedPin = AppContainer.requirePrefsManager().getLogPIN(),
                onPinChanged = { newPin ->
                    AppContainer.requirePrefsManager().setLogPIN(newPin)
                    LoggingManager.logInfo(
                        component = "SettingsScreen",
                        action = "CHANGE_PIN",
                        message = "Log-PIN wurde geändert"
                    )
                    SnackbarManager.showSuccess(context.getString(R.string.msg_pin_changed))
                    showChangePinDialog = false
                },
                onDismiss = { showChangePinDialog = false }
            )
        }
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = 0.8f },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Header (always visible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandChange(!expanded) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Einklappen" else "Ausklappen",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Content (expandable)
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        thickness = 1.dp
                    )
                    Box(modifier = Modifier.padding(16.dp)) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
private fun CallForwardingSettingsSection(
    viewModel: ContactsViewModel,
    sectionTitleStyle: androidx.compose.ui.text.TextStyle,
    onFocusChanged: (Boolean) -> Unit
        ) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.hint_mmi_sim_alignment),
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
        )

        MmiSimSelectionSection(
            viewModel = viewModel,
            sectionTitleStyle = sectionTitleStyle
        )

        MmiCodeSettingsSection(
            viewModel = viewModel,
            onFocusChanged = onFocusChanged,
            sectionTitleStyle = sectionTitleStyle,
            showMmiWarningToggle = true
        )
    }
}

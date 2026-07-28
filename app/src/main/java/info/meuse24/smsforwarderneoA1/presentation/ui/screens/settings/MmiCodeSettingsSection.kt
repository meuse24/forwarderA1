package info.meuse24.smsforwarderneoA1.presentation.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import info.meuse24.smsforwarderneoA1.R
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.ContactsViewModel
import info.meuse24.smsforwarderneoA1.domain.model.MmiCodeProfile
import info.meuse24.smsforwarderneoA1.domain.model.A1Detection

@Composable
fun MmiCodeSettingsSection(
    viewModel: ContactsViewModel,
    onFocusChanged: (Boolean) -> Unit,
    sectionTitleStyle: TextStyle
) {
    val mmiActivatePrefix by viewModel.mmiActivatePrefix.collectAsState()
    val mmiActivateSuffix by viewModel.mmiActivateSuffix.collectAsState()
    val mmiDeactivateCode by viewModel.mmiDeactivateCode.collectAsState()
    val mmiStatusCode by viewModel.mmiStatusCode.collectAsState()
    val mmiCodeProfile by viewModel.mmiCodeProfile.collectAsState()
    val a1Detection by viewModel.mmiSimA1Detection.collectAsState()

    var isActivateFocused by remember { mutableStateOf(false) }
    var isActivateSuffixFocused by remember { mutableStateOf(false) }
    var isDeactivateFocused by remember { mutableStateOf(false) }
    var isStatusFocused by remember { mutableStateOf(false) }
    var showA1ProfileConfirmation by remember { mutableStateOf(false) }
    var showStandardProfileConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(isActivateFocused, isActivateSuffixFocused, isDeactivateFocused, isStatusFocused) {
        onFocusChanged(isActivateFocused || isActivateSuffixFocused || isDeactivateFocused || isStatusFocused)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.section_mmi_ussd_codes),
            style = sectionTitleStyle,
            color = androidx.compose.material3.MaterialTheme.colorScheme.primary
        )
        if (a1Detection == A1Detection.A1_CONFIRMED) {
            Text(
                text = stringResource(R.string.mmi_a1_detected_hint),
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            text = stringResource(
                when (mmiCodeProfile) {
                    MmiCodeProfile.STANDARD_GSM -> R.string.mmi_profile_standard
                    MmiCodeProfile.A1_SPECIAL -> R.string.mmi_profile_a1_special
                    MmiCodeProfile.CUSTOM -> R.string.mmi_profile_custom
                }
            ),
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = mmiActivatePrefix,
            onValueChange = { viewModel.updateMmiActivatePrefix(it) },
            label = { Text(stringResource(R.string.label_mmi_activate_prefix)) },
            placeholder = { Text(stringResource(R.string.placeholder_mmi_prefix)) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isActivateFocused = it.isFocused }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = mmiActivateSuffix,
            onValueChange = { viewModel.updateMmiActivateSuffix(it) },
            label = { Text(stringResource(R.string.label_mmi_activate_suffix)) },
            placeholder = { Text(stringResource(R.string.placeholder_mmi_suffix)) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isActivateSuffixFocused = it.isFocused }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = mmiDeactivateCode,
            onValueChange = { viewModel.updateMmiDeactivateCode(it) },
            label = { Text(stringResource(R.string.label_mmi_deactivate_code)) },
            placeholder = { Text(stringResource(R.string.placeholder_mmi_deactivate)) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isDeactivateFocused = it.isFocused }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = mmiStatusCode,
            onValueChange = { viewModel.updateMmiStatusCode(it) },
            label = { Text(stringResource(R.string.label_mmi_status_code)) },
            placeholder = { Text(stringResource(R.string.placeholder_mmi_status)) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isStatusFocused = it.isFocused }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { showA1ProfileConfirmation = true }
            ) {
                Text(stringResource(R.string.btn_preset_a1_special))
            }

            Button(
                onClick = { showStandardProfileConfirmation = true }
            ) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.btn_preset_standard),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.btn_preset_standard))
            }
        }

        if (showA1ProfileConfirmation) {
            AlertDialog(
                onDismissRequest = { showA1ProfileConfirmation = false },
                title = { Text(stringResource(R.string.mmi_a1_special_confirm_title)) },
                text = { Text(stringResource(R.string.mmi_a1_special_confirm_body)) },
                confirmButton = {
                    Button(onClick = {
                        viewModel.resetMmiCodesToDefault()
                        showA1ProfileConfirmation = false
                    }) { Text(stringResource(R.string.mmi_a1_special_confirm_apply)) }
                },
                dismissButton = {
                    Button(onClick = { showA1ProfileConfirmation = false }) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                },
            )
        }
        if (showStandardProfileConfirmation) {
            AlertDialog(
                onDismissRequest = { showStandardProfileConfirmation = false },
                title = { Text(stringResource(R.string.mmi_standard_confirm_title)) },
                text = { Text(stringResource(R.string.mmi_standard_confirm_body)) },
                confirmButton = {
                    Button(onClick = {
                        viewModel.resetMmiCodesToGeneric()
                        showStandardProfileConfirmation = false
                    }) { Text(stringResource(R.string.mmi_standard_confirm_apply)) }
                },
                dismissButton = {
                    Button(onClick = { showStandardProfileConfirmation = false }) { Text(stringResource(R.string.btn_cancel)) }
                },
            )
        }

    }
}

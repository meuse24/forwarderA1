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
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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

@Composable
fun MmiCodeSettingsSection(
    viewModel: ContactsViewModel,
    onFocusChanged: (Boolean) -> Unit,
    sectionTitleStyle: TextStyle,
    showMmiWarningToggle: Boolean = false
) {
    val mmiActivatePrefix by viewModel.mmiActivatePrefix.collectAsState()
    val mmiActivateSuffix by viewModel.mmiActivateSuffix.collectAsState()
    val mmiDeactivateCode by viewModel.mmiDeactivateCode.collectAsState()
    val mmiStatusCode by viewModel.mmiStatusCode.collectAsState()
    val mmiWarningEnabled by viewModel.mmiWarningEnabled.collectAsState()

    var isActivateFocused by remember { mutableStateOf(false) }
    var isActivateSuffixFocused by remember { mutableStateOf(false) }
    var isDeactivateFocused by remember { mutableStateOf(false) }
    var isStatusFocused by remember { mutableStateOf(false) }

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
                onClick = { viewModel.resetMmiCodesToDefault() }
            ) {
                Text(stringResource(R.string.btn_preset_a1_austria))
            }

            Button(
                onClick = { viewModel.resetMmiCodesToGeneric() }
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

        Spacer(modifier = Modifier.height(24.dp))

        if (showMmiWarningToggle) {
            // MMI Warning Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.toggle_mmi_warning),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.desc_mmi_warning),
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = mmiWarningEnabled,
                    onCheckedChange = { viewModel.updateMmiWarningEnabled(it) }
                )
            }
        }
    }
}

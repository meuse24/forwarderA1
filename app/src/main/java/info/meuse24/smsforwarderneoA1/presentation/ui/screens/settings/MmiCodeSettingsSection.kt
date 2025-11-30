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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.ContactsViewModel

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
            text = "MMI-Codes (Anrufweiterleitung)",
            style = sectionTitleStyle,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = mmiActivatePrefix,
            onValueChange = { viewModel.updateMmiActivatePrefix(it) },
            label = { Text("Aktivierungscode Prefix (z.B. *21*)") },
            placeholder = { Text("*21*") },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isActivateFocused = it.isFocused }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = mmiActivateSuffix,
            onValueChange = { viewModel.updateMmiActivateSuffix(it) },
            label = { Text("Aktivierungscode Suffix (z.B. **)") },
            placeholder = { Text("**") },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isActivateSuffixFocused = it.isFocused }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = mmiDeactivateCode,
            onValueChange = { viewModel.updateMmiDeactivateCode(it) },
            label = { Text("Deaktivierungscode (z.B. **21**)") },
            placeholder = { Text("**21**") },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isDeactivateFocused = it.isFocused }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = mmiStatusCode,
            onValueChange = { viewModel.updateMmiStatusCode(it) },
            label = { Text("Statusabfrage (z.B. *021**)") },
            placeholder = { Text("*021**") },
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
                Text("A1(BMI)")
            }

            Button(
                onClick = { viewModel.resetMmiCodesToGeneric() }
            ) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "Reset",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Standard")
            }
        }
    }
}

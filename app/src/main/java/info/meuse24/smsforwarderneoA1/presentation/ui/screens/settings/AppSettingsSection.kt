package info.meuse24.smsforwarderneoA1.presentation.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.ContactsViewModel
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.EmailViewModel
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.TestUtilsViewModel
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.NavigationViewModel

@Composable
fun AppSettingsSection(
    viewModel: ContactsViewModel,
    emailViewModel: EmailViewModel,
    testUtilsViewModel: TestUtilsViewModel,
    navigationViewModel: NavigationViewModel,
    onFocusChanged: (Boolean) -> Unit,
    sectionTitleStyle: TextStyle
) {
    val filterText by viewModel.filterText.collectAsState()
    val testSmsText by testUtilsViewModel.testSmsText.collectAsState()
    val testEmailText by emailViewModel.testEmailText.collectAsState()
    val topBarTitle by navigationViewModel.topBarTitle.collectAsState()
    val mailScreenVisible by viewModel.mailScreenVisible.collectAsState()
    val phoneNumberFormatting by viewModel.phoneNumberFormatting.collectAsState()

    var isFilterTextFocused by remember { mutableStateOf(false) }
    var isTestSmsTextFocused by remember { mutableStateOf(false) }
    var isTestEmailTextFocused by remember { mutableStateOf(false) }
    var isTopBarTitleFocused by remember { mutableStateOf(false) }

    LaunchedEffect(
        isFilterTextFocused,
        isTestSmsTextFocused,
        isTestEmailTextFocused,
        isTopBarTitleFocused
    ) {
        onFocusChanged(
            isFilterTextFocused || isTestSmsTextFocused ||
                    isTestEmailTextFocused || isTopBarTitleFocused
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "App-Einstellungen",
            style = sectionTitleStyle,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = filterText,
            onValueChange = { viewModel.updateFilterText(it) },
            label = { Text("Kontakte - Suchfilter") },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFilterTextFocused = it.isFocused }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = testSmsText,
            onValueChange = { testUtilsViewModel.updateTestSmsText(it) },
            label = { Text("Text der Test-SMS") },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isTestSmsTextFocused = it.isFocused }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = testEmailText,
            onValueChange = { emailViewModel.updateTestEmailText(it) },
            label = { Text("Text der Test-Email") },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isTestEmailTextFocused = it.isFocused }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = topBarTitle,
            onValueChange = { navigationViewModel.updateTopBarTitle(it) },
            label = { Text("App Titel") },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isTopBarTitleFocused = it.isFocused }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Mail Screen Visibility Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Mail-Tab anzeigen",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Zeigt den Mail-Tab in der unteren Navigation an",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = mailScreenVisible,
                onCheckedChange = { viewModel.updateMailScreenVisibility(it) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Phone Number Formatting Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Telefonnummern formatieren",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Formatiert Telefonnummern beim Einlesen der Kontakte",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = phoneNumberFormatting,
                onCheckedChange = { viewModel.updatePhoneNumberFormatting(it) }
            )
        }
    }
}

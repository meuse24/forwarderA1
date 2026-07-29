package info.meuse24.smsforwarderneoA1.presentation.ui.screens.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.meuse24.smsforwarderneoA1.R

/**
 * Privacy Policy Screen - Shown before permission requests
 *
 * Displays bilingual (German/English) privacy policy explaining:
 * - Required permissions and their purpose
 * - Data handling and security
 * - User consent requirement
 */
@Composable
fun PrivacyPolicyScreen(
    onAccept: (() -> Unit)? = null,
    onDecline: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewMode: Boolean = false
) {
    var selectedLanguage by remember { mutableStateOf("DE") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp)
            ) {
                // Header
                PrivacyPolicyHeader(
                    selectedLanguage = selectedLanguage,
                    onLanguageChange = { selectedLanguage = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        if (selectedLanguage == "DE") {
                            PrivacyPolicyContentDE()
                        } else {
                            PrivacyPolicyContentEN()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                if (viewMode) {
                    // View-Only Mode: Nur "OK" Button
                    Button(
                        onClick = { onDismiss?.invoke() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.btn_ok),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // First Launch Mode: Akzeptieren/Ablehnen Buttons
                    PrivacyPolicyActions(
                        onAccept = onAccept ?: {},
                        onDecline = onDecline ?: {},
                        selectedLanguage = selectedLanguage
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacyPolicyHeader(
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = stringResource(R.string.desc_dialing_privacy),
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (selectedLanguage == "DE")
                stringResource(R.string.privacy_title_de) else stringResource(R.string.privacy_title_en),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Language Toggle
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedLanguage == "DE",
                onClick = { onLanguageChange("DE") },
                label = { Text(stringResource(R.string.language_german)) }
            )
            FilterChip(
                selected = selectedLanguage == "EN",
                onClick = { onLanguageChange("EN") },
                label = { Text(stringResource(R.string.language_english)) }
            )
        }
    }
}

@Composable
private fun PrivacyPolicyContentDE() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PolicySection(
            title = stringResource(R.string.privacy_section_welcome_title_de),
            content = stringResource(R.string.privacy_section_welcome_body_de)
        )

        PolicySection(
            title = stringResource(R.string.privacy_section_permissions_title_de),
            content = null
        )

        PermissionItem(
            title = stringResource(R.string.privacy_perm_sms_de),
            description = stringResource(R.string.privacy_perm_sms_body_de)
        )

        PermissionItem(
            title = stringResource(R.string.privacy_perm_phone_de),
            description = stringResource(R.string.privacy_perm_phone_body_de)
        )

        PermissionItem(
            title = stringResource(R.string.privacy_perm_foreground_de),
            description = stringResource(R.string.privacy_perm_foreground_body_de)
        )

        PermissionItem(
            title = stringResource(R.string.privacy_perm_battery_de),
            description = stringResource(R.string.privacy_perm_battery_body_de)
        )

        PolicySection(
            title = stringResource(R.string.privacy_section_security_title_de),
            content = stringResource(R.string.privacy_section_security_body_de)
        )

        PolicySection(
            title = stringResource(R.string.privacy_section_email_title_de),
            content = stringResource(R.string.privacy_section_email_body_de)
        )

        PolicySection(
            title = stringResource(R.string.privacy_section_opensource_title_de),
            content = stringResource(R.string.privacy_section_opensource_body_de)
        )

        PolicySection(
            title = stringResource(R.string.privacy_section_loop_protection_title_de),
            content = stringResource(R.string.privacy_section_loop_protection_body_de)
        )

        PolicySection(
            title = stringResource(R.string.privacy_section_consent_title_de),
            content = stringResource(R.string.privacy_section_consent_body_de)
        )
    }
}

@Composable
private fun PrivacyPolicyContentEN() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PolicySection(
            title = stringResource(R.string.privacy_section_welcome_title_en),
            content = stringResource(R.string.privacy_section_welcome_body_en)
        )

        PolicySection(
            title = stringResource(R.string.privacy_section_permissions_title_en),
            content = null
        )

        PermissionItem(
            title = stringResource(R.string.privacy_perm_sms_en),
            description = stringResource(R.string.privacy_perm_sms_body_en)
        )

        PermissionItem(
            title = stringResource(R.string.privacy_perm_phone_en),
            description = stringResource(R.string.privacy_perm_phone_body_en)
        )

        PermissionItem(
            title = stringResource(R.string.privacy_perm_foreground_en),
            description = stringResource(R.string.privacy_perm_foreground_body_en)
        )

        PermissionItem(
            title = stringResource(R.string.privacy_perm_battery_en),
            description = stringResource(R.string.privacy_perm_battery_body_en)
        )

        PolicySection(
            title = stringResource(R.string.privacy_section_security_title_en),
            content = stringResource(R.string.privacy_section_security_body_en)
        )

        PolicySection(
            title = stringResource(R.string.privacy_section_email_title_en),
            content = stringResource(R.string.privacy_section_email_body_en)
        )

        PolicySection(
            title = stringResource(R.string.privacy_section_opensource_title_en),
            content = stringResource(R.string.privacy_section_opensource_body_en)
        )

        PolicySection(
            title = stringResource(R.string.privacy_section_loop_protection_title_en),
            content = stringResource(R.string.privacy_section_loop_protection_body_en)
        )

        PolicySection(
            title = stringResource(R.string.privacy_section_consent_title_en),
            content = stringResource(R.string.privacy_section_consent_body_en)
        )
    }
}

@Composable
private fun PolicySection(
    title: String,
    content: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        if (content != null) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PrivacyPolicyActions(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    selectedLanguage: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onAccept,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (selectedLanguage == "DE") stringResource(R.string.privacy_btn_accept_de) else stringResource(
                    R.string.privacy_btn_accept_en
                ),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedButton(
            onClick = onDecline,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(
                text = if (selectedLanguage == "DE") stringResource(R.string.privacy_btn_decline_de) else stringResource(
                    R.string.privacy_btn_decline_en
                ),
                fontSize = 16.sp
            )
        }
    }
}

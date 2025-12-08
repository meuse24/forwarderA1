package info.meuse24.smsforwarderneoA1.presentation.ui.screens.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
                        text = "OK",
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
            contentDescription = "Privacy",
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (selectedLanguage == "DE")
                "Datenschutzerklärung" else "Privacy Policy",
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
            title = "Willkommen bei SMS Forwarder Neo",
            content = """
                Diese App leitet eingehende SMS-Nachrichten automatisch weiter. Bevor Sie fortfahren,
                lesen Sie bitte diese Datenschutzerklärung sorgfältig durch, um zu verstehen,
                welche Berechtigungen erforderlich sind und wie Ihre Daten behandelt werden.
            """.trimIndent()
        )

        PolicySection(
            title = "1. Erforderliche Berechtigungen",
            content = null
        )

        PermissionItem(
            title = "SMS senden und empfangen",
            description = """
                • SMS_RECEIVE: Zum Empfang eingehender SMS-Nachrichten
                • SMS_SEND: Zum Weiterleiten von SMS an die konfigurierte Zielrufnummer

                Dies ist die Kernfunktionalität der App. Ohne diese Berechtigung kann die
                SMS-Weiterleitung nicht funktionieren.
            """.trimIndent()
        )

        PermissionItem(
            title = "Telefon / Anrufe",
            description = """
                • CALL_PHONE: Zum Ausführen von MMI-Codes (*21*, ##21#, *#21#)
                • READ_PHONE_STATE: Zum Erkennen der SIM-Karte und Netzwerkstatus

                MMI-Codes werden verwendet, um den Status der Anrufweiterleitung direkt beim
                Mobilfunkanbieter abzufragen. Dies ist optional, verbessert aber die
                Zuverlässigkeit der App.
            """.trimIndent()
        )

        PermissionItem(
            title = "Kontakte lesen",
            description = """
                • READ_CONTACTS: Zum Öffnen des Android-Kontaktauswahlmenüs

                Die App speichert nur den ausgewählten Kontakt (Name + Nummer) lokal und
                verschlüsselt. Es werden keine Kontaktdaten an Dritte weitergegeben.
            """.trimIndent()
        )

        PermissionItem(
            title = "Vordergrund-Dienst",
            description = """
                • FOREGROUND_SERVICE: Für zuverlässige Hintergrundverarbeitung
                • FOREGROUND_SERVICE_PHONE_CALL: Speziell für SMS/Telefon-Funktionen

                Der Vordergrund-Dienst stellt sicher, dass SMS auch bei gesperrtem Bildschirm
                weitergeleitet werden. Sie sehen eine Benachrichtigung, wenn der Dienst aktiv ist.
            """.trimIndent()
        )

        PermissionItem(
            title = "Akku-Optimierung ignorieren",
            description = """
                • REQUEST_IGNORE_BATTERY_OPTIMIZATIONS: Für kontinuierlichen Dienst

                Android kann Apps im Hintergrund stoppen, um Akku zu sparen. Diese Berechtigung
                verhindert, dass der SMS-Weiterleitungsdienst unerwartet beendet wird.
            """.trimIndent()
        )

        PolicySection(
            title = "2. Datenverarbeitung und Sicherheit",
            content = """
                • Alle Einstellungen werden verschlüsselt auf Ihrem Gerät gespeichert
                • SMTP-Passwörter werden mit androidx.security.crypto verschlüsselt
                • Telefonnummern werden nur lokal protokolliert (für Debugging)
                • Es werden keine Daten an externe Server gesendet (außer bei E-Mail-Weiterleitung an Ihren SMTP-Server)
                • Die App ist für den privaten Gebrauch konzipiert
            """.trimIndent()
        )

        PolicySection(
            title = "3. E-Mail-Weiterleitung (optional)",
            content = """
                Wenn Sie die E-Mail-Weiterleitung aktivieren, werden SMS-Inhalte an die von
                Ihnen konfigurierten E-Mail-Adressen gesendet. Dies erfordert die Eingabe von
                SMTP-Zugangsdaten (Gmail, Outlook, etc.), die verschlüsselt gespeichert werden.
            """.trimIndent()
        )

        PolicySection(
            title = "4. Open Source und Transparenz",
            content = """
                Diese App ist Open Source. Der vollständige Quellcode ist verfügbar unter:
                https://github.com/meuse24/forwarderA1

                Sie können den Code einsehen und prüfen, dass keine versteckten Funktionen
                oder Datensammlung vorhanden sind.
            """.trimIndent()
        )

        PolicySection(
            title = "5. Ihre Zustimmung",
            content = """
                Durch Klicken auf "Akzeptieren" bestätigen Sie, dass Sie:

                • Diese Datenschutzerklärung gelesen und verstanden haben
                • Der Verwendung der oben genannten Berechtigungen zustimmen
                • Verstehen, dass die App SMS-Inhalte weiterleitet
                • Die App auf eigene Verantwortung nutzen

                Sie können die App jederzeit deinstallieren, um alle lokalen Daten zu entfernen.
            """.trimIndent()
        )
    }
}

@Composable
private fun PrivacyPolicyContentEN() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PolicySection(
            title = "Welcome to SMS Forwarder Neo",
            content = """
                This app automatically forwards incoming SMS messages. Before proceeding,
                please read this privacy policy carefully to understand what permissions
                are required and how your data is handled.
            """.trimIndent()
        )

        PolicySection(
            title = "1. Required Permissions",
            content = null
        )

        PermissionItem(
            title = "Send and Receive SMS",
            description = """
                • SMS_RECEIVE: To receive incoming SMS messages
                • SMS_SEND: To forward SMS to the configured target number

                This is the core functionality of the app. Without this permission,
                SMS forwarding cannot work.
            """.trimIndent()
        )

        PermissionItem(
            title = "Phone / Calls",
            description = """
                • CALL_PHONE: To execute MMI codes (*21*, ##21#, *#21#)
                • READ_PHONE_STATE: To detect SIM card and network status

                MMI codes are used to query call forwarding status directly from your
                mobile carrier. This is optional but improves app reliability.
            """.trimIndent()
        )

        PermissionItem(
            title = "Read Contacts",
            description = """
                • READ_CONTACTS: To open the Android contact picker dialog

                The app only stores the selected contact (name + number) locally and
                encrypted. No contact data is shared with third parties.
            """.trimIndent()
        )

        PermissionItem(
            title = "Foreground Service",
            description = """
                • FOREGROUND_SERVICE: For reliable background processing
                • FOREGROUND_SERVICE_PHONE_CALL: Specifically for SMS/phone functions

                The foreground service ensures SMS are forwarded even when the screen is locked.
                You will see a notification when the service is active.
            """.trimIndent()
        )

        PermissionItem(
            title = "Ignore Battery Optimization",
            description = """
                • REQUEST_IGNORE_BATTERY_OPTIMIZATIONS: For continuous service

                Android may stop background apps to save battery. This permission prevents
                the SMS forwarding service from being unexpectedly terminated.
            """.trimIndent()
        )

        PolicySection(
            title = "2. Data Processing and Security",
            content = """
                • All settings are encrypted and stored on your device
                • SMTP passwords are encrypted using androidx.security.crypto
                • Phone numbers are only logged locally (for debugging)
                • No data is sent to external servers (except email forwarding to your SMTP server)
                • The app is designed for private use
            """.trimIndent()
        )

        PolicySection(
            title = "3. Email Forwarding (Optional)",
            content = """
                If you enable email forwarding, SMS contents will be sent to the email
                addresses you configure. This requires entering SMTP credentials
                (Gmail, Outlook, etc.), which are stored encrypted.
            """.trimIndent()
        )

        PolicySection(
            title = "4. Open Source and Transparency",
            content = """
                This app is open source. The complete source code is available at:
                https://github.com/meuse24/forwarderA1

                You can review the code and verify that there are no hidden features
                or data collection.
            """.trimIndent()
        )

        PolicySection(
            title = "5. Your Consent",
            content = """
                By clicking "Accept" you confirm that you:

                • Have read and understood this privacy policy
                • Agree to the use of the permissions listed above
                • Understand that the app forwards SMS contents
                • Use the app at your own responsibility

                You can uninstall the app at any time to remove all local data.
            """.trimIndent()
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
                text = if (selectedLanguage == "DE") "Akzeptieren" else "Accept",
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
                text = if (selectedLanguage == "DE") "Ablehnen" else "Decline",
                fontSize = 16.sp
            )
        }
    }
}

package info.meuse24.smsforwarderneoA1.presentation.ui.screens.info

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import info.meuse24.smsforwarderneoA1.BuildConfig
import info.meuse24.smsforwarderneoA1.R
import info.meuse24.smsforwarderneoA1.presentation.ui.components.GradientBorderCard
import info.meuse24.smsforwarderneoA1.ui.theme.BackgroundGradientLight
import info.meuse24.smsforwarderneoA1.ui.theme.PrimaryGradient

@Composable
fun InfoScreen() {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5
    val packageInfo = remember {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGradientLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Logo-Sektion mit Gradient Border
            GradientBorderCard(
                modifier = Modifier.fillMaxWidth(),
                borderWidth = 3.dp,
                gradient = PrimaryGradient,
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logofwd2),
                            contentDescription = "App Icon",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(scaleX = 1.5f, scaleY = 1.5f)
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "(C) 2025",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Günther Meusburger",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Version ${packageInfo.versionName} (${packageInfo.longVersionCode})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Build: ${BuildConfig.BUILD_TIME}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Links-Sektion
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Links",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    LinkItem(
                        icon = Icons.Outlined.Language,
                        title = "Author Website",
                        subtitle = "www.meuse24.info",
                        url = "https://www.meuse24.info",
                        context = context
                    )

                    LinkItem(
                        icon = Icons.Outlined.Code,
                        title = "GitHub Repository",
                        subtitle = "Source Code & Issues",
                        url = "https://github.com/meuse24/forwarderA1",
                        context = context
                    )

                    LinkItem(
                        icon = Icons.Outlined.Language,
                        title = "Project Website",
                        subtitle = "Documentation & Downloads",
                        url = "https://meuse24.github.io/forwarderA1/",
                        context = context
                    )
                }
            }

            // HTML-Content Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.apply {
                                javaScriptEnabled = false
                                builtInZoomControls = true
                                displayZoomControls = false
                            }
                            webViewClient = WebViewClient()
                            setBackgroundColor(if (isDarkTheme) 0xFF121212.toInt() else 0xFFFFFFFF.toInt())
                            loadDataWithBaseURL(
                                null,
                                getHtmlContent(isDarkTheme, context),
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL(
                            null,
                            getHtmlContent(isDarkTheme, context),
                            "text/html",
                            "UTF-8",
                            null
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun getHtmlContent(isDarkTheme: Boolean, context: Context): String {
    val backgroundColor = if (isDarkTheme) "#121212" else "#FFFFFF"
    val textColor = if (isDarkTheme) "#E0E0E0" else "#333333"

    val currentAndroidVersion = Build.VERSION.RELEASE
    val currentSDKVersion = Build.VERSION.SDK_INT
    val minSDKVersion = context.applicationInfo.minSdkVersion
    val targetSDKVersion = context.applicationInfo.targetSdkVersion

    return """
<!DOCTYPE html>
<html lang="de">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen-Sans, Ubuntu, Cantarell, 'Helvetica Neue', sans-serif;
            font-size: 14px;
            line-height: 1.5;
            color: $textColor;
            background-color: $backgroundColor;
            margin: 0;
            padding: 10px;
        }
        h2 {
            font-size: 16px;
            margin-top: 20px;
            margin-bottom: 10px;
            color: ${if (isDarkTheme) "#E0E0E0" else "#333333"};
        }
        .section-container {
            background-color: ${if (isDarkTheme) "#1E1E1E" else "#F5F5F5"};
            border-radius: 8px;
            padding: 16px;
            margin-bottom: 16px;
        }
        .info-item {
            margin-bottom: 16px;
            word-wrap: break-word;
            overflow-wrap: break-word;
        }
        .info-label {
            font-weight: 500;
            font-size: 12px;
            color: ${if (isDarkTheme) "#9E9E9E" else "#666666"};
            margin-bottom: 4px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }
        .info-value {
            color: ${if (isDarkTheme) "#E0E0E0" else "#333333"};
            font-size: 14px;
            line-height: 1.4;
        }
        .badge {
            display: inline-block;
            padding: 2px 8px;
            border-radius: 4px;
            font-size: 12px;
            background-color: ${if (isDarkTheme) "#333333" else "#E0E0E0"};
            color: ${if (isDarkTheme) "#E0E0E0" else "#333333"};
        }
        .feature-list {
            list-style-type: none;
            padding: 0;
            margin: 0;
        }
        .feature-item {
            margin-bottom: 24px;
        }
        .feature-title {
            font-weight: 600;
            color: ${if (isDarkTheme) "#E0E0E0" else "#333333"};
            margin-bottom: 8px;
        }
        .feature-description {
            color: ${if (isDarkTheme) "#B0B0B0" else "#666666"};
            margin-left: 16px;
        }
        .footnote {
            font-style: italic;
            margin-top: 16px;
            color: ${if (isDarkTheme) "#9E9E9E" else "#666666"};
        }
    </style>
</head>
<body>
    <div class="section-container">
        <h2>Hauptfunktionen</h2>
        <ul class="feature-list">
            <li class="feature-item">
                <div class="feature-title">Kontaktauswahl per Android Contact Picker</div>
                <div class="feature-description">
                    Die App nutzt den nativen Android Contact Picker für eine einfache und schnelle Kontaktauswahl:
                    <ul>
                        <li>Ein-Klick Auswahl über den nativen Android Contact Picker</li>
                        <li>Persistente verschlüsselte Speicherung der Auswahl</li>
                        <li>Übersichtliche Kontaktkarte mit Name, Nummer und Typ-Anzeige</li>
                        <li>Schnelle Änderung oder Deaktivierung des ausgewählten Kontakts</li>
                        <li>Integrierte Test-SMS Funktion direkt aus der Kontaktkarte</li>
                        <li>Status-Abfrage für aktive Weiterleitungen</li>
                        <li>Zentrale Zurücksetzung aller Weiterleitungen</li>
                    </ul>
                </div>
            </li>
            
            <li class="feature-item">
                <div class="feature-title">Weiterleitung von SMS</div>
                <div class="feature-description">
                    Die SMS-Weiterleitung bietet zwei unabhängige Weiterleitungswege, die Sie einzeln oder kombiniert nutzen können:
                    <ul>
                        <li>SMS-zu-SMS: Direkte Weiterleitung an eine ausgewählte Telefonnummer</li>
                        <li>SMS-zu-Email: Weiterleitung an beliebig viele Email-Adressen</li>
                        <li>Formatierte Weiterleitungen mit Absender und Zeitstempel</li>
                        <li>Test-Funktionen für beide Weiterleitungswege</li>
                    </ul>
                </div>
            </li>
            
            <li class="feature-item">
                <div class="feature-title">Email-Konfiguration</div>
                <div class="feature-description">
                    Die Email-Weiterleitung bietet umfangreiche Konfigurationsmöglichkeiten:
                    <ul>
                        <li>Unterstützung für verschiedene Email-Provider (Gmail, eigene SMTP-Server)</li>
                        <li>Verschlüsselte Speicherung der Zugangsdaten</li>
                        <li>Individuelle Test-Emails an jede konfigurierte Adresse</li>
                        <li>Detaillierte Fehlerdiagnose bei Zustellungsproblemen</li>
                    </ul>
                </div>
            </li>

            <li class="feature-item">
                <div class="feature-title">SIM-Karten Auswahl (Dual-SIM Unterstützung)</div>
                <div class="feature-description">
                    Für Geräte mit mehreren SIM-Karten bietet die App umfassende Verwaltungsmöglichkeiten:
                    <ul>
                        <li>Automatische Erkennung aller verfügbaren SIM-Karten</li>
                        <li>Flexible SIM-Auswahl für SMS-Weiterleitung (Automatisch, SIM 1, SIM 2)</li>
                        <li>Übersichtliche Anzeige der SIM-Karten Details (Anbieter, Rufnummer)</li>
                        <li>Individuelle Konfiguration für jede SIM-Karte</li>
                    </ul>
                </div>
            </li>

            <li class="feature-item">
                <div class="feature-title">Anrufweiterleitung</div>
                <div class="feature-description">
                    Die Anrufweiterleitung nutzt die native Telefonfunktion Ihres Geräts:
                    <ul>
                        <li>Automatische Weiterleitung aller eingehenden Anrufe</li>
                        <li>Verwendung von MMI/USSD-Codes für maximale Kompatibilität</li>
                        <li>Gleichzeitige Aktivierung mit SMS-Weiterleitung</li>
                        <li>Status-Anzeige der aktiven Weiterleitung</li>
                    </ul>
                </div>
            </li>

            <li class="feature-item">
                <div class="feature-title">MMI Code Einstellungen</div>
                <div class="feature-description">
                    Erweiterte Konfiguration für Mobilfunk-Weiterleitungen via MMI-Codes:
                    <ul>
                        <li>Konfigurierbare MMI/USSD-Codes für Anrufweiterleitung</li>
                        <li>Netzwerkspezifische Anpassungen (Standard, Telekom, Vodafone, etc.)</li>
                        <li>Status-Abfragen der aktiven Netzwerk-Weiterleitungen</li>
                        <li>Automatische Zurücksetzung aller Weiterleitungen</li>
                    </ul>
                </div>
            </li>

            <li class="feature-item">
                <div class="feature-title">Test und Überwachung</div>
                <div class="feature-description">
                    Umfangreiche Test- und Überwachungsfunktionen für alle Weiterleitungen:
                    <ul>
                        <li>Test-SMS mit anpassbarem Inhalt</li>
                        <li>Separate Test-Emails an einzelne Adressen</li>
                        <li>Detaillierte Protokollierung aller Aktivitäten</li>
                        <li>Export von Protokollen im CSV-Format</li>
                    </ul>
                </div>
            </li>
            
            <li class="feature-item">
                <div class="feature-title">Benutzeroberfläche</div>
                <div class="feature-description">
                    Die Benutzeroberfläche wurde für intuitive Bedienung optimiert:
                    <ul>
                        <li>Separate Bereiche für SMS- und Email-Konfiguration</li>
                        <li>Übersichtliche Statusanzeigen</li>
                        <li>Farbkodierte Protokollansicht</li>
                        <li>Anpassung an Tablets und Querformat</li>
                    </ul>
                </div>
            </li>
            
            <li class="feature-item">
                <div class="feature-title">Sicherheit und Datenschutz</div>
                <div class="feature-description">
                    Die App legt besonderen Wert auf Sicherheit und Datenschutz:
                    <ul>
                        <li>Verschlüsselte Speicherung sensibler Daten</li>
                        <li>Sichere Handhabung von Email-Zugangsdaten</li>
                        <li>Automatische Bereinigung beim Beenden</li>
                        <li>Keine dauerhafte Speicherung von SMS-Inhalten</li>
                    </ul>
                </div>
            </li>
        </ul>
    </div>

    <div class="section-container">
        <h2>System-Information</h2>

        <div class="info-item">
            <div class="info-label">Programmiersprache</div>
            <div class="info-value">Kotlin ${BuildConfig.KOTLIN_VERSION}</div>
        </div>

        <div class="info-item">
            <div class="info-label">UI-Framework</div>
            <div class="info-value">Jetpack Compose</div>
        </div>

        <div class="info-item">
            <div class="info-label">Build-System</div>
            <div class="info-value">Gradle ${BuildConfig.GRADLE_VERSION} mit AGP ${BuildConfig.AGP_VERSION}</div>
        </div>

        <div class="info-item">
            <div class="info-label">Build Tools</div>
            <div class="info-value">${BuildConfig.BUILD_TOOLS_VERSION}</div>
        </div>

        <div class="info-item">
            <div class="info-label">Build Zeitpunkt</div>
            <div class="info-value">${BuildConfig.BUILD_TIME}</div>
        </div>

        <div class="info-item">
            <div class="info-label">Build Typ</div>
            <div class="info-value"><span class="badge">${BuildConfig.BUILD_TYPE}</span></div>
        </div>

        <div class="info-item">
            <div class="info-label">Architektur</div>
            <div class="info-value">MVVM (Model-View-ViewModel) mit Repository-Pattern</div>
        </div>

        <div class="info-item">
            <div class="info-label">Nebenläufigkeit</div>
            <div class="info-value">Coroutines und Flow</div>
        </div>

        <div class="info-item">
            <div class="info-label">Datenspeicherung</div>
            <div class="info-value">Verschlüsselte SharedPreferences</div>
        </div>

        <div class="info-item">
            <div class="info-label">Hintergrunddienst</div>
            <div class="info-value">Foreground Service</div>
        </div>

        <div class="info-item">
            <div class="info-label">Android Version</div>
            <div class="info-value">Android $currentAndroidVersion (API Level $currentSDKVersion)</div>
        </div>

        <div class="info-item">
            <div class="info-label">Min SDK</div>
            <div class="info-value">Android ${getAndroidVersionName(minSDKVersion)} (API Level $minSDKVersion)</div>
        </div>

        <div class="info-item">
            <div class="info-label">Target SDK</div>
            <div class="info-value">Android ${getAndroidVersionName(targetSDKVersion)} (API Level $targetSDKVersion)</div>
        </div>

        <div class="info-item">
            <div class="info-label">JDK</div>
            <div class="info-value">${BuildConfig.JDK_VERSION}</div>
        </div>
    </div>

    <p class="footnote">
        Die App wurde unter Berücksichtigung moderner Android-Entwicklungspraktiken und Datenschutzrichtlinien entwickelt. 
        Sie läuft energieeffizient im Hintergrund und gewährleistet dabei eine zuverlässige Weiterleitung Ihrer Nachrichten und Anrufe.
    </p>
</body>
</html>
""".trimIndent()
}

@Composable
private fun LinkItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    url: String,
    context: Context
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.Outlined.OpenInNew,
            contentDescription = "Öffnen",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun getAndroidVersionName(sdkInt: Int): String {
    return when (sdkInt) {
        Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> "14"  // API 34
        Build.VERSION_CODES.TIRAMISU -> "13"          // API 33
        Build.VERSION_CODES.S_V2 -> "12L/12.1"        // API 32
        Build.VERSION_CODES.S -> "12"                 // API 31
        Build.VERSION_CODES.R -> "11"                 // API 30
        Build.VERSION_CODES.Q -> "10"                 // API 29
        Build.VERSION_CODES.P -> "9"                  // API 28
        Build.VERSION_CODES.O_MR1 -> "8.1"           // API 27
        Build.VERSION_CODES.O -> "8.0"               // API 26
        Build.VERSION_CODES.N_MR1 -> "7.1"           // API 25
        Build.VERSION_CODES.N -> "7.0"               // API 24
        Build.VERSION_CODES.M -> "6.0"               // API 23
        else -> "Version $sdkInt"
    }
}

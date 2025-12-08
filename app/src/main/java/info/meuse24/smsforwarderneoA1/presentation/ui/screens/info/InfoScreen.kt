package info.meuse24.smsforwarderneoA1.presentation.ui.screens.info

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import info.meuse24.smsforwarderneoA1.BuildConfig
import info.meuse24.smsforwarderneoA1.R
import info.meuse24.smsforwarderneoA1.presentation.ui.components.GradientBorderCard
import info.meuse24.smsforwarderneoA1.presentation.ui.screens.privacy.PrivacyPolicyScreen
import info.meuse24.smsforwarderneoA1.ui.theme.PrimaryGradient

@Composable
fun InfoScreen() {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5
    val packageInfo = remember {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    var showPrivacyPolicy by remember { mutableStateOf(false) }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Logo-Sektion mit Gradient Border
            GradientBorderCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = 0.8f },
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
                    // Logos Column (App Logo + Barracuda Logo)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // App Logo
                        Box(
                            modifier = Modifier
                                .size(80.dp)
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

                        // Barracuda Logo
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.barracuda),
                                contentDescription = "Version Barracuda",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
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
                            text = "${stringResource(R.string.label_version)} ${packageInfo.versionName} (${packageInfo.longVersionCode})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${stringResource(R.string.label_build)} ${BuildConfig.BUILD_TIME}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Links-Sektion
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = 0.8f },
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
                        text = stringResource(R.string.heading_links),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    LinkItem(
                        icon = Icons.Outlined.Language,
                        title = stringResource(R.string.link_author_website),
                        subtitle = stringResource(R.string.link_author_website_url),
                        url = "https://www.meuse24.info",
                        context = context
                    )

                    LinkItem(
                        icon = Icons.Outlined.Code,
                        title = stringResource(R.string.link_github_repo),
                        subtitle = stringResource(R.string.link_github_subtitle),
                        url = "https://github.com/meuse24/forwarderA1",
                        context = context
                    )

                    LinkItem(
                        icon = Icons.Outlined.Language,
                        title = stringResource(R.string.link_project_website),
                        subtitle = stringResource(R.string.link_project_subtitle),
                        url = "https://meuse24.github.io/forwarderA1/",
                        context = context
                    )
                }
            }

            // Datenschutzerklärung Button
            Button(
                onClick = { showPrivacyPolicy = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = 0.95f },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = stringResource(R.string.btn_privacy_policy),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.btn_privacy_policy),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // HTML-Content Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = 0.8f },
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

        // Privacy Policy Dialog
        if (showPrivacyPolicy) {
            Dialog(
                onDismissRequest = { showPrivacyPolicy = false },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false
                )
            ) {
                PrivacyPolicyScreen(
                    viewMode = true,
                    onDismiss = { showPrivacyPolicy = false }
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

    // Get current locale
    val locale = context.resources.configuration.locales[0].language

    return """
<!DOCTYPE html>
<html lang="$locale">
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
        <h2>${context.getString(R.string.html_heading_main_features)}</h2>
        <ul class="feature-list">
            <li class="feature-item">
                <div class="feature-title">${context.getString(R.string.html_feature_contact_picker)}</div>
                <div class="feature-description">
                    ${context.getString(R.string.html_feature_contact_picker_intro)}
                    <ul>
                        <li>${context.getString(R.string.html_feature_contact_picker_1)}</li>
                        <li>${context.getString(R.string.html_feature_contact_picker_2)}</li>
                        <li>${context.getString(R.string.html_feature_contact_picker_3)}</li>
                        <li>${context.getString(R.string.html_feature_contact_picker_4)}</li>
                        <li>${context.getString(R.string.html_feature_contact_picker_5)}</li>
                        <li>${context.getString(R.string.html_feature_contact_picker_6)}</li>
                        <li>${context.getString(R.string.html_feature_contact_picker_7)}</li>
                    </ul>
                </div>
            </li>

            <li class="feature-item">
                <div class="feature-title">${context.getString(R.string.html_feature_sms_forwarding)}</div>
                <div class="feature-description">
                    ${context.getString(R.string.html_feature_sms_forwarding_intro)}
                    <ul>
                        <li>${context.getString(R.string.html_feature_sms_forwarding_1)}</li>
                        <li>${context.getString(R.string.html_feature_sms_forwarding_2)}</li>
                        <li>${context.getString(R.string.html_feature_sms_forwarding_3)}</li>
                        <li>${context.getString(R.string.html_feature_sms_forwarding_4)}</li>
                    </ul>
                </div>
            </li>

            <li class="feature-item">
                <div class="feature-title">${context.getString(R.string.html_feature_email_config)}</div>
                <div class="feature-description">
                    ${context.getString(R.string.html_feature_email_config_intro)}
                    <ul>
                        <li>${context.getString(R.string.html_feature_email_config_1)}</li>
                        <li>${context.getString(R.string.html_feature_email_config_2)}</li>
                        <li>${context.getString(R.string.html_feature_email_config_3)}</li>
                        <li>${context.getString(R.string.html_feature_email_config_4)}</li>
                    </ul>
                </div>
            </li>

            <li class="feature-item">
                <div class="feature-title">${context.getString(R.string.html_feature_sim_selection)}</div>
                <div class="feature-description">
                    ${context.getString(R.string.html_feature_sim_selection_intro)}
                    <ul>
                        <li>${context.getString(R.string.html_feature_sim_selection_1)}</li>
                        <li>${context.getString(R.string.html_feature_sim_selection_2)}</li>
                        <li>${context.getString(R.string.html_feature_sim_selection_3)}</li>
                        <li>${context.getString(R.string.html_feature_sim_selection_4)}</li>
                    </ul>
                </div>
            </li>

            <li class="feature-item">
                <div class="feature-title">${context.getString(R.string.html_feature_call_forwarding)}</div>
                <div class="feature-description">
                    ${context.getString(R.string.html_feature_call_forwarding_intro)}
                    <ul>
                        <li>${context.getString(R.string.html_feature_call_forwarding_1)}</li>
                        <li>${context.getString(R.string.html_feature_call_forwarding_2)}</li>
                        <li>${context.getString(R.string.html_feature_call_forwarding_3)}</li>
                        <li>${context.getString(R.string.html_feature_call_forwarding_4)}</li>
                    </ul>
                </div>
            </li>

            <li class="feature-item">
                <div class="feature-title">${context.getString(R.string.html_feature_mmi_codes)}</div>
                <div class="feature-description">
                    ${context.getString(R.string.html_feature_mmi_codes_intro)}
                    <ul>
                        <li>${context.getString(R.string.html_feature_mmi_codes_1)}</li>
                        <li>${context.getString(R.string.html_feature_mmi_codes_2)}</li>
                        <li>${context.getString(R.string.html_feature_mmi_codes_3)}</li>
                        <li>${context.getString(R.string.html_feature_mmi_codes_4)}</li>
                    </ul>
                </div>
            </li>

            <li class="feature-item">
                <div class="feature-title">${context.getString(R.string.html_feature_testing)}</div>
                <div class="feature-description">
                    ${context.getString(R.string.html_feature_testing_intro)}
                    <ul>
                        <li>${context.getString(R.string.html_feature_testing_1)}</li>
                        <li>${context.getString(R.string.html_feature_testing_2)}</li>
                        <li>${context.getString(R.string.html_feature_testing_3)}</li>
                        <li>${context.getString(R.string.html_feature_testing_4)}</li>
                    </ul>
                </div>
            </li>

            <li class="feature-item">
                <div class="feature-title">${context.getString(R.string.html_feature_ui)}</div>
                <div class="feature-description">
                    ${context.getString(R.string.html_feature_ui_intro)}
                    <ul>
                        <li>${context.getString(R.string.html_feature_ui_1)}</li>
                        <li>${context.getString(R.string.html_feature_ui_2)}</li>
                        <li>${context.getString(R.string.html_feature_ui_3)}</li>
                        <li>${context.getString(R.string.html_feature_ui_4)}</li>
                    </ul>
                </div>
            </li>

            <li class="feature-item">
                <div class="feature-title">${context.getString(R.string.html_feature_security)}</div>
                <div class="feature-description">
                    ${context.getString(R.string.html_feature_security_intro)}
                    <ul>
                        <li>${context.getString(R.string.html_feature_security_1)}</li>
                        <li>${context.getString(R.string.html_feature_security_2)}</li>
                        <li>${context.getString(R.string.html_feature_security_3)}</li>
                        <li>${context.getString(R.string.html_feature_security_4)}</li>
                    </ul>
                </div>
            </li>
        </ul>
    </div>

    <div class="section-container">
        <h2>${context.getString(R.string.html_heading_system_info)}</h2>

        <div class="info-item">
            <div class="info-label">${context.getString(R.string.html_label_language)}</div>
            <div class="info-value">${context.getString(R.string.html_value_kotlin)} ${BuildConfig.KOTLIN_VERSION}</div>
        </div>

        <div class="info-item">
            <div class="info-label">${context.getString(R.string.html_label_ui_framework)}</div>
            <div class="info-value">${context.getString(R.string.html_value_jetpack_compose)}</div>
        </div>

        <div class="info-item">
            <div class="info-label">${context.getString(R.string.html_label_build_system)}</div>
            <div class="info-value">${context.getString(R.string.html_value_gradle)} ${BuildConfig.GRADLE_VERSION} mit AGP ${BuildConfig.AGP_VERSION}</div>
        </div>

        <div class="info-item">
            <div class="info-label">${context.getString(R.string.html_label_build_tools)}</div>
            <div class="info-value">${BuildConfig.BUILD_TOOLS_VERSION}</div>
        </div>

        <div class="info-item">
            <div class="info-label">${context.getString(R.string.html_label_build_time)}</div>
            <div class="info-value">${BuildConfig.BUILD_TIME}</div>
        </div>

        <div class="info-item">
            <div class="info-label">${context.getString(R.string.html_label_build_type)}</div>
            <div class="info-value"><span class="badge">${BuildConfig.BUILD_TYPE}</span></div>
        </div>

        <div class="info-item">
            <div class="info-label">${context.getString(R.string.html_label_architecture)}</div>
            <div class="info-value">${context.getString(R.string.html_value_architecture)}</div>
        </div>

        <div class="info-item">
            <div class="info-label">${context.getString(R.string.html_label_concurrency)}</div>
            <div class="info-value">${context.getString(R.string.html_value_concurrency)}</div>
        </div>

        <div class="info-item">
            <div class="info-label">${context.getString(R.string.html_label_data_storage)}</div>
            <div class="info-value">${context.getString(R.string.html_value_encrypted_prefs)}</div>
        </div>

        <div class="info-item">
            <div class="info-label">${context.getString(R.string.html_label_background_service)}</div>
            <div class="info-value">${context.getString(R.string.html_value_foreground_service)}</div>
        </div>

        <div class="info-item">
            <div class="info-label">${context.getString(R.string.html_label_android_version)}</div>
            <div class="info-value">Android $currentAndroidVersion (API Level $currentSDKVersion)</div>
        </div>

        <div class="info-item">
            <div class="info-label">${context.getString(R.string.html_label_min_sdk)}</div>
            <div class="info-value">Android ${getAndroidVersionName(minSDKVersion)} (API Level $minSDKVersion)</div>
        </div>

        <div class="info-item">
            <div class="info-label">${context.getString(R.string.html_label_target_sdk)}</div>
            <div class="info-value">Android ${getAndroidVersionName(targetSDKVersion)} (API Level $targetSDKVersion)</div>
        </div>

        <div class="info-item">
            <div class="info-label">${context.getString(R.string.html_label_jdk)}</div>
            <div class="info-value">${BuildConfig.JDK_VERSION}</div>
        </div>
    </div>

    <p class="footnote">
        ${context.getString(R.string.html_footnote_development)}
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
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
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
            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
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

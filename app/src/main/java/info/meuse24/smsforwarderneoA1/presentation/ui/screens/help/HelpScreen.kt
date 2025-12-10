package info.meuse24.smsforwarderneoA1.presentation.ui.screens.help

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import info.meuse24.smsforwarderneoA1.R
import java.io.ByteArrayOutputStream

@Composable
fun HelpScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5

    // Handle system back button to return to the home screen instead of closing the app
    BackHandler(onBack = onNavigateBack)

    // Cache Base64-konvertierte Bilder (nur 1x konvertieren pro Session)
    val cachedImages = remember {
        mapOf(
            "select" to drawableToBase64(context, R.drawable.help_btn_select),
            "status" to drawableToBase64(context, R.drawable.help_btn_status),
            "reset" to drawableToBase64(context, R.drawable.help_btn_reset)
        )
    }

    Box(
        modifier = modifier.fillMaxSize()
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
                .padding(16.dp)
        ) {
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
                                setSupportZoom(true)
                            }
                            webViewClient = WebViewClient()
                            setBackgroundColor(if (isDarkTheme) 0xFF121212.toInt() else 0xFFFFFFFF.toInt())
                            loadDataWithBaseURL(
                                null,
                                getHelpHtmlContent(isDarkTheme, cachedImages, context),
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL(
                            null,
                            getHelpHtmlContent(isDarkTheme, cachedImages, context),
                            "text/html",
                            "UTF-8",
                            null
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Zurück-Button (oben rechts)
        FloatingActionButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.btn_back),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun drawableToBase64(context: Context, drawableId: Int): String {
    val bitmap = BitmapFactory.decodeResource(context.resources, drawableId)
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    val byteArray = outputStream.toByteArray()
    return Base64.encodeToString(byteArray, Base64.NO_WRAP)
}

private fun getHelpHtmlContent(
    isDarkTheme: Boolean,
    cachedImages: Map<String, String>,
    context: Context
): String {
    val backgroundColor = if (isDarkTheme) "#121212" else "#FFFFFF"
    val textColor = if (isDarkTheme) "#E0E0E0" else "#333333"
    val primaryColor = if (isDarkTheme) "#90CAF9" else "#0056b3"
    val cardBg = if (isDarkTheme) "#1E1E1E" else "#f7f9fc"
    val borderColor = if (isDarkTheme) "#424242" else "#e1e7f0"
    val noteBackground = if (isDarkTheme) "#1A3A52" else "#e7f3fe"
    val noteBorder = if (isDarkTheme) "#64B5F6" else "#2196F3"
    val highlightBg = if (isDarkTheme) "#5C1A1A" else "#ffcccc"
    val highlightText = if (isDarkTheme) "#FF6B6B" else "#a80000"

    // Use cached Base64 images
    val btnSelectBase64 = cachedImages["select"] ?: ""
    val btnStatusBase64 = cachedImages["status"] ?: ""
    val btnResetBase64 = cachedImages["reset"] ?: ""

    val locale = context.resources.configuration.locales[0].language

    return """
<!DOCTYPE html>
<html lang="$locale">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=yes">
    <title>${context.getString(R.string.help_html_title)}</title>
    <style>
        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
            font-size: 14px;
            line-height: 1.6;
            color: $textColor;
            background-color: $backgroundColor;
            padding: 12px;
        }
        h1 {
            font-size: 20px;
            color: $primaryColor;
            border-bottom: 2px solid $primaryColor;
            padding-bottom: 8px;
            margin-bottom: 16px;
            text-align: center;
        }
        h2 {
            font-size: 16px;
            color: $primaryColor;
            letter-spacing: 0.3px;
            text-transform: uppercase;
            margin: 20px 0 12px;
            padding-bottom: 6px;
            border-bottom: 1px solid $borderColor;
        }
        h3 {
            font-size: 14px;
            color: $primaryColor;
            margin: 12px 0 6px;
            font-weight: 600;
        }
        .step {
            display: flex;
            gap: 10px;
            margin-bottom: 16px;
            padding: 12px;
            border-radius: 8px;
            background: $cardBg;
            border: 1px solid $borderColor;
        }
        .step-number {
            font-weight: bold;
            color: $primaryColor;
            font-size: 16px;
            min-width: 24px;
            flex-shrink: 0;
        }
        .step-content {
            flex: 1;
        }
        .step-content strong {
            display: block;
            margin-bottom: 6px;
            font-size: 15px;
        }
        code {
            background-color: $borderColor;
            padding: 2px 6px;
            border-radius: 4px;
            font-family: 'Courier New', Courier, monospace;
            font-size: 13px;
        }
        .note {
            background-color: $noteBackground;
            border-left: 4px solid $noteBorder;
            padding: 12px;
            margin: 16px 0;
            border-radius: 4px;
            font-size: 13px;
            line-height: 1.5;
        }
        .note strong {
            display: block;
            margin-bottom: 6px;
        }
        .highlight-red {
            background-color: $highlightBg;
            color: $highlightText;
            padding: 10px;
            margin: 12px 0;
            border-radius: 6px;
            font-weight: 500;
            font-size: 13px;
            line-height: 1.5;
        }
        .features-table {
            width: 100%;
            margin-top: 8px;
        }
        .feature-row {
            display: flex;
            gap: 12px;
            padding: 10px 0;
            border-bottom: 1px solid $borderColor;
        }
        .feature-row:last-child {
            border-bottom: none;
        }
        .feature-label {
            font-weight: 600;
            color: $primaryColor;
            min-width: 100px;
            flex-shrink: 0;
        }
        .feature-description {
            flex: 1;
            line-height: 1.5;
        }
        .button-icon {
            display: inline-block;
            height: 18px;
            vertical-align: middle;
            margin: 0 4px;
            border-radius: 3px;
        }
        .step-icon {
            display: block;
            max-height: 40px;
            max-width: 160px;
            margin: 6px 0;
            border-radius: 6px;
        }
        @media (max-width: 600px) {
            body {
                font-size: 13px;
                padding: 8px;
            }
            h1 {
                font-size: 18px;
            }
            h2 {
                font-size: 15px;
            }
            .step {
                padding: 10px;
            }
            .feature-row {
                flex-direction: column;
                gap: 4px;
            }
            .feature-label {
                min-width: auto;
            }
        }
    </style>
</head>
<body>
    <h1>${context.getString(R.string.help_html_heading)}</h1>

    <p style="margin-bottom: 16px; text-align: center; font-style: italic;">
        ${context.getString(R.string.help_html_intro)}
    </p>

    <h2>${context.getString(R.string.help_html_steps_heading)}</h2>

    <div class="step">
        <span class="step-number">1.</span>
        <div class="step-content">
            <strong>${context.getString(R.string.help_step1_title)}</strong>
            ${context.getString(R.string.help_step1_body, context.getString(R.string.btn_select_contact))}
            <br><br>
            <img src="data:image/png;base64,$btnSelectBase64" class="step-icon" alt="${context.getString(R.string.btn_select_contact)}">
        </div>
    </div>

    <div class="step">
        <span class="step-number">2.</span>
        <div class="step-content">
            <strong>${context.getString(R.string.help_step2_title)}</strong>
            ${context.getString(R.string.help_step2_body)}
            <div class="highlight-red">
                ${context.getString(R.string.help_step2_warning)}
            </div>
        </div>
    </div>

    <div class="step">
        <span class="step-number">3.</span>
        <div class="step-content">
            <strong>${context.getString(R.string.help_step3_title)}</strong>
            ${context.getString(R.string.help_step3_body, context.getString(R.string.btn_deactivate))}
        </div>
    </div>

    <div class="note">
        <strong>${context.getString(R.string.help_note_title)}</strong>
        ${context.getString(R.string.help_note_body)}
    </div>

    <h2>${context.getString(R.string.help_buttons_heading)}</h2>

    <div class="features-table">
        <div class="feature-row">
            <div class="feature-label">${context.getString(R.string.help_btn_change_contact_title)}</div>
            <div class="feature-description">
                ${context.getString(R.string.help_btn_change_contact_desc)}
            </div>
        </div>

        <div class="feature-row">
            <div class="feature-label">${context.getString(R.string.help_btn_test_sms_title)}</div>
            <div class="feature-description">
                ${context.getString(R.string.help_btn_test_sms_desc)}
            </div>
        </div>

        <div class="feature-row">
            <div class="feature-label">
                <img src="data:image/png;base64,$btnStatusBase64" class="button-icon" alt="${context.getString(R.string.help_btn_status_title)}">
                ${context.getString(R.string.help_btn_status_title)}
            </div>
            <div class="feature-description">
                ${context.getString(R.string.help_btn_status_desc)}
            </div>
        </div>

        <div class="feature-row">
            <div class="feature-label">
                <img src="data:image/png;base64,$btnResetBase64" class="button-icon" alt="${context.getString(R.string.help_btn_reset_title)}">
                ${context.getString(R.string.help_btn_reset_title)}
            </div>
            <div class="feature-description">
                ${context.getString(R.string.help_btn_reset_desc)}
            </div>
        </div>
    </div>

    <h2>${context.getString(R.string.help_email_heading)}</h2>

    <p style="margin-bottom: 12px; line-height: 1.5;">
        ${context.getString(R.string.help_email_intro)}
    </p>

    <div class="step">
        <span class="step-number">1.</span>
        <div class="step-content">
            <strong>${context.getString(R.string.help_email_step1_title)}</strong>
            ${context.getString(R.string.help_email_step1_body)}
        </div>
    </div>

    <div class="step">
        <span class="step-number">2.</span>
        <div class="step-content">
            <strong>${context.getString(R.string.help_email_step2_title)}</strong>
            ${context.getString(R.string.help_email_step2_body)}
        </div>
    </div>

    <div class="step">
        <span class="step-number">3.</span>
        <div class="step-content">
            <strong>${context.getString(R.string.help_email_step3_title)}</strong>
            ${context.getString(R.string.help_email_step3_body)}
        </div>
    </div>

    <div class="note">
        ${context.getString(R.string.help_email_note)}
    </div>

</body>
</html>
""".trimIndent()
}

package info.meuse24.smsforwarderneoA1.presentation.ui.screens.home

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.R

@Composable
fun FilterAndLogo(
    filterText: String,
    onFilterTextChange: (String) -> Unit,
    forwardingActive: Boolean,
    onDeactivateForwarding: () -> Unit
) {
    val rotation = remember { Animatable(0f) }
    var hasAnimated by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    var isFilterFocused by remember { mutableStateOf(false) }

    // Animiere nur beim ersten Start, nicht bei jedem Recompose
    LaunchedEffect(Unit) {
        if (!hasAnimated) {
            rotation.animateTo(
                targetValue = 360f,
                animationSpec = tween(
                    durationMillis = 2000,
                    easing = LinearEasing
                )
            )
            hasAnimated = true
        }
    }

// Speichere den Orientierungszustand
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 4.dp,
                horizontal = if (isLandscape) 8.dp else 4.dp
            )
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .graphicsLayer(rotationZ = rotation.value)  // Keine Animation, nur der Wert
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logofwd2),
                    contentDescription = "App Icon",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(scaleX = 1.5f, scaleY = 1.5f)
                        .align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = filterText,
                    onValueChange = onFilterTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged {
                            isFilterFocused = it.isFocused
                        },
                    label = { Text("Kontakt suchen") },
                    placeholder = { Text("Namen oder Nummer eingeben") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                        }
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Suchen",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (filterText.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    onFilterTextChange("")
                                    focusManager.clearFocus()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Filter löschen",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}

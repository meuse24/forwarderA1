package info.meuse24.smsforwarderneoA1.presentation.ui.screens.mail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.SnackbarManager
import info.meuse24.smsforwarderneoA1.presentation.ui.components.AnimatedOutlinedButton
import info.meuse24.smsforwarderneoA1.presentation.ui.components.GradientBorderCard
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.EmailViewModel
import info.meuse24.smsforwarderneoA1.ui.theme.AnimationDuration
import info.meuse24.smsforwarderneoA1.ui.theme.BackgroundGradientLight
import info.meuse24.smsforwarderneoA1.ui.theme.PrimaryGradient
import info.meuse24.smsforwarderneoA1.ui.theme.SuccessGradient

@Composable
fun MailScreen(emailViewModel: EmailViewModel) {
    val emailAddresses by emailViewModel.emailAddresses.collectAsState()
    val newEmailAddress by emailViewModel.newEmailAddress.collectAsState()
    val forwardSmsToEmail by emailViewModel.forwardSmsToEmail.collectAsState()
    var isEmailAddressFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Scale animation for Add button
    val addButtonScale by animateFloatAsState(
        targetValue = if (newEmailAddress.isNotEmpty()) 1.05f else 1f,
        animationSpec = tween(durationMillis = AnimationDuration.FAST),
        label = "add_button_scale"
    )

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
            // Checkbox Card mit Gradient Border
            GradientBorderCard(
                modifier = Modifier.fillMaxWidth(),
                borderWidth = 2.dp,
                gradient = if (forwardSmsToEmail) SuccessGradient else PrimaryGradient,
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = forwardSmsToEmail,
                        onCheckedChange = { checked ->
                            if (emailAddresses.isNotEmpty()) {
                                emailViewModel.updateForwardSmsToEmail(checked)
                            } else if (checked) {
                                SnackbarManager.showWarning("Bitte fügen Sie zuerst E-Mail-Adressen hinzu")
                            }
                        },
                        enabled = emailAddresses.isNotEmpty()
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "SMS an alle E-Mails weiterleiten",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (emailAddresses.isNotEmpty())
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Eingabefeld und Add-Button Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = newEmailAddress,
                        onValueChange = { emailViewModel.updateNewEmailAddress(it) },
                        label = { Text("Neue E-Mail-Adresse") },
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { isEmailAddressFocused = it.isFocused },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                emailViewModel.addEmailAddress()
                            }
                        )
                    )

                    AnimatedOutlinedButton(
                        onClick = {
                            focusManager.clearFocus()
                            emailViewModel.addEmailAddress()
                        },
                        modifier = Modifier.scale(addButtonScale)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Hinzufügen",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // E-Mail-Liste mit Animationen
            AnimatedVisibility(
                visible = emailAddresses.isNotEmpty(),
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(durationMillis = AnimationDuration.NORMAL)
                ) + fadeIn(animationSpec = tween(durationMillis = AnimationDuration.NORMAL)),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(durationMillis = AnimationDuration.FAST)
                ) + fadeOut(animationSpec = tween(durationMillis = AnimationDuration.FAST))
            ) {
                GradientBorderCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderWidth = 2.dp,
                    gradient = PrimaryGradient,
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        emailAddresses.forEachIndexed { index, email ->
                            EmailListItem(
                                email = email,
                                onTestEmail = { emailViewModel.sendTestEmail(email) },
                                onDelete = { emailViewModel.removeEmailAddress(email) }
                            )
                            if (index < emailAddresses.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                }
            }

            // Empty state
            if (emailAddresses.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Keine E-Mail-Adressen vorhanden",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmailListItem(
    email: String,
    onTestEmail: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = email,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            AnimatedOutlinedButton(
                onClick = onTestEmail
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Test-Mail senden",
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            AnimatedOutlinedButton(
                onClick = onDelete
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Löschen",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

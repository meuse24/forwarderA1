package info.meuse24.smsforwarderneoA1.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.provider.Telephony
import android.telephony.SmsMessage
import androidx.core.app.NotificationCompat
import info.meuse24.smsforwarderneoA1.AppContainer
import info.meuse24.smsforwarderneoA1.BuildConfig
import info.meuse24.smsforwarderneoA1.LoggingManager
import info.meuse24.smsforwarderneoA1.MainActivity
import info.meuse24.smsforwarderneoA1.PhoneNumberValidator
import info.meuse24.smsforwarderneoA1.PhoneSmsUtils
import info.meuse24.smsforwarderneoA1.R
import info.meuse24.smsforwarderneoA1.SnackbarManager
import info.meuse24.smsforwarderneoA1.data.local.SharedPreferencesManager
import info.meuse24.smsforwarderneoA1.domain.model.SimSelectionMode
import info.meuse24.smsforwarderneoA1.util.email.EmailResult
import info.meuse24.smsforwarderneoA1.util.email.EmailSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.mail.MessagingException

data class SmsMessagePart(
    val body: String,
    val timestamp: Long,
    val referenceNumber: Int,
    val sequencePosition: Int,
    val totalParts: Int,
    val sender: String,
    val subscriptionId: Int = -1  // -1 = unbekannt/nicht verfügbar
)

class SmsForegroundService : Service() {
    private val prefsManager: SharedPreferencesManager by lazy {
        AppContainer.requirePrefsManager()
    }
    private var currentNotificationText = "App läuft im Hintergrund."
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: throw IllegalStateException("NotificationManager nicht verfügbar")
    }
    private var wakeLock: PowerManager.WakeLock? = null
    private val wakeLockMutex = Mutex()
    private val wakeLockTimeout = 5 * 60 * 1000L // 5 Minuten Maximum
    private val wakeLockTag = "${BuildConfig.APPLICATION_ID}:ForegroundService"

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "MY_CHANNEL_ID"
        private const val DEFAULT_NOTIFICATION_TEXT = "TEL/SMS Forwarder läuft im Hintergrund."
        private const val MAX_RETRIES = 3
        private const val INITIAL_RETRY_DELAY = 5000L  // 5 Sekunden

        @Volatile
        private var isRunning = false

        // Retry-Counter für fehlerhafte SMS-Verarbeitungen
        private val retryCounter = mutableMapOf<String, Int>()

        fun startService(context: Context) {
            if (!isRunning) {
                val intent = Intent(context, SmsForegroundService::class.java)
                context.startForegroundService(intent)
            }
        }

        fun stopService(context: Context) {
            if (isRunning) {
                context.stopService(Intent(context, SmsForegroundService::class.java))
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        try {
            setupService()
        } catch (e: Exception) {
            LoggingManager.logError(
                component = "SmsForegroundService",
                action = "CREATE",
                message = "Service creation failed",
                error = e
            )
            stopSelf()
        }
    }

    private fun setupService() {
        val notification = createNotification(DEFAULT_NOTIFICATION_TEXT)
        startForeground(NOTIFICATION_ID, notification)
        isRunning = true

        LoggingManager.logInfo(
            component = "SmsForegroundService",
            action = "START",
            message = "Service started successfully"
        )
    }

    private suspend fun withWakeLock(timeout: Long = wakeLockTimeout, block: suspend () -> Unit) {
        wakeLockMutex.withLock {
            try {
                acquireWakeLock(timeout)
                withTimeout(timeout) {
                    block()
                }
            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "SmsForegroundService",
                    action = "WAKE_LOCK_ERROR",
                    message = "Fehler während WakeLock-Operation",
                    error = e
                )
                throw e
            } finally {
                releaseWakeLock()
            }
        }
    }

    private fun acquireWakeLock(timeout: Long) {
        try {
            releaseWakeLock() // Bestehenden WakeLock freigeben

            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                ?: throw IllegalStateException("PowerManager nicht verfügbar")
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                wakeLockTag
            ).apply {
                setReferenceCounted(false) // WakeLock wird nicht referenzgezählt
                acquire(minOf(timeout, wakeLockTimeout))
            }

            LoggingManager.logInfo(
                component = "SmsForegroundService",
                action = "WAKE_LOCK_ACQUIRED",
                message = "WakeLock aktiviert",
                details = mapOf("timeout_ms" to timeout)
            )
        } catch (e: Exception) {
            LoggingManager.logError(
                component = "SmsForegroundService",
                action = "WAKE_LOCK_ERROR",
                message = "WakeLock konnte nicht aktiviert werden",
                error = e
            )
            wakeLock = null
            throw e
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { lock ->
            try {
                if (lock.isHeld) {
                    lock.release()
                    LoggingManager.logInfo(
                        component = "SmsForegroundService",
                        action = "WAKE_LOCK_RELEASED",
                        message = "WakeLock freigegeben"
                    )
                }
            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "SmsForegroundService",
                    action = "WAKE_LOCK_RELEASE_ERROR",
                    message = "Fehler beim Freigeben des WakeLock",
                    error = e
                )
            } finally {
                wakeLock = null
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            when (intent?.action) {
                "START_SERVICE" -> {
                    if (!isRunning) {
                        startForegroundService()
                    }
                }

                "PROCESS_SMS" -> {
                    if (!isRunning) {
                        startForegroundService()
                    }

                    intent.extras?.let { extras ->
                        serviceScope.launch {
                            processSmsData(extras)
                        }
                    }
                }

                "UPDATE_NOTIFICATION" -> {
                    val newText = intent.getStringExtra("contentText")
                    if (newText != null && newText != currentNotificationText) {
                        updateNotification(newText)
                    }
                }

                null -> {
                    // Service wurde nach System-Kill neugestartet
                    if (!isRunning) {
                        startForegroundService()
                        LoggingManager.logInfo(
                            component = "SmsForegroundService",
                            action = "RESTART",
                            message = "Service nach System-Kill neugestartet"
                        )
                    }
                }

                else -> {
                    LoggingManager.logWarning(
                        component = "SmsForegroundService",
                        action = "UNKNOWN_COMMAND",
                        message = "Unbekannte Action empfangen",
                        details = mapOf("action" to (intent.action ?: "null"))
                    )
                }
            }

            return START_STICKY

        } catch (e: Exception) {
            LoggingManager.logError(
                component = "SmsForegroundService",
                action = "START_COMMAND_ERROR",
                message = "Fehler im onStartCommand",
                error = e
            )
            stopSelf()
            return START_NOT_STICKY
        }
    }

    private suspend fun processSmsData(extras: Bundle) {
        withWakeLock(2 * 60 * 1000L) {
            try {
                // Extrahiere Subscription ID aus Intent (für Multi-SIM-Support)
                val subscriptionId = extras.getInt("subscription", -1)

                val smsIntent = Intent().apply {
                    action = Telephony.Sms.Intents.SMS_RECEIVED_ACTION
                    putExtras(extras)
                }

                val messages = Telephony.Sms.Intents.getMessagesFromIntent(smsIntent)
                    ?.takeIf { it.isNotEmpty() }
                    ?: run {
                        LoggingManager.logWarning(
                            component = "SmsForegroundService",
                            action = "PROCESS_SMS",
                            message = "Keine gültigen SMS-Nachrichten im Intent"
                        )
                        return@withWakeLock
                    }

                val messageParts = messages.mapNotNull { smsMessage ->
                    smsMessage?.originatingAddress?.let { sender ->
                        smsMessage.messageBody?.let { body ->
                            SmsMessagePart(
                                body = body,
                                timestamp = smsMessage.timestampMillis,
                                referenceNumber = smsMessage.messageRef,
                                sequencePosition = smsMessage.indexOnIcc,
                                totalParts = messages.size,
                                sender = sender,
                                subscriptionId = subscriptionId
                            )
                        }
                    }
                }.takeIf { it.isNotEmpty() } ?: run {
                    LoggingManager.logWarning(
                        component = "SmsForegroundService",
                        action = "PROCESS_SMS",
                        message = "Keine gültigen Nachrichtenteile extrahiert"
                    )
                    return@withWakeLock
                }

                val messageGroups = messageParts.groupBy {
                    "${it.sender}_${it.referenceNumber}"
                }

                messageGroups.forEach { (key, parts) ->
                    val sender = key.substringBefore('_')
                    processMessageGroup(sender, parts)
                }

                LoggingManager.logInfo(
                    component = "SmsForegroundService",
                    action = "PROCESS_SMS",
                    message = "SMS-Verarbeitung abgeschlossen",
                    details = mapOf(
                        "messages_count" to messages.size,
                        "groups_count" to messageGroups.size,
                        "subscription_id" to subscriptionId
                    )
                )

            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "SmsForegroundService",
                    action = "PROCESS_SMS_ERROR",
                    message = "Fehler bei SMS-Verarbeitung",
                    error = e
                )
                SnackbarManager.showError("Fehler bei der SMS-Verarbeitung: ${e.message}")
            }
        }
    }

    private suspend fun processMessageGroup(sender: String, parts: List<SmsMessagePart>) {
        try {
            val orderedParts = parts.sortedWith(
                compareBy<SmsMessagePart> { it.sequencePosition }
                    .thenBy { it.timestamp }
            )

            val fullMessage = orderedParts.joinToString("") { it.body }

            LoggingManager.logDebug(
                component = "SmsForegroundService",
                action = "PROCESS_MESSAGE_GROUP",
                message = "Verarbeite SMS-Gruppe",
                details = mapOf(
                    "sender" to sender,
                    "parts_count" to parts.size,
                    "total_length" to fullMessage.length,
                    "is_multipart" to (parts.size > 1),
                    "subscription_id" to (parts.firstOrNull()?.subscriptionId ?: -1)
                )
            )

            // Parallele Ausführung von SMS- und Email-Weiterleitung
            coroutineScope {
                // SMS Weiterleitung
                if (prefsManager.isForwardingActive()) {
                    launch {
                        prefsManager.getSelectedPhoneNumber().let { forwardToNumber ->
                            // Loop Protection: Verhindere Weiterleitung, wenn Absender == Zielnummer
                            val validator = PhoneNumberValidator(applicationContext)
                            if (validator.areSameNumber(sender, forwardToNumber)) {
                                LoggingManager.logWarning(
                                    component = "SmsForegroundService",
                                    action = "LOOP_PROTECTION",
                                    message = "Weiterleitung gestoppt: Absender entspricht Zielrufnummer",
                                    details = mapOf("number" to sender)
                                )
                                return@launch
                            }

                            val incomingSubscriptionId = parts.firstOrNull()?.subscriptionId ?: -1
                            val forwardedMessage = buildForwardedSmsMessage(sender, fullMessage, incomingSubscriptionId)
                            withContext(Dispatchers.IO) {
                                // Bestimme Ziel-SIM basierend auf Benutzereinstellung
                                val targetSubscriptionId = determineTargetSubscriptionId(incomingSubscriptionId)
                                forwardSmsWithSubscription(forwardToNumber, forwardedMessage, targetSubscriptionId)
                            }
                        }
                    }
                }

                // Email Weiterleitung
                if (prefsManager.isForwardSmsToEmail()) {
                    launch {
                        withContext(Dispatchers.IO) {
                            handleEmailForwarding(sender, fullMessage)
                        }
                    }
                }
            }

        } catch (e: Exception) {
            LoggingManager.logError(
                component = "SmsForegroundService",
                action = "PROCESS_GROUP_ERROR",
                message = "Fehler bei der Verarbeitung einer SMS-Gruppe",
                error = e,
                details = mapOf(
                    "sender" to sender,
                    "parts_count" to parts.size
                )
            )
            handleProcessingError(sender, parts, e)
        }
    }

    private fun handleProcessingError(
        sender: String,
        parts: List<SmsMessagePart>,
        error: Exception
    ) {
        try {
            LoggingManager.logError(
                component = "SmsForegroundService",
                action = "HANDLE_ERROR",
                message = "SMS-Verarbeitung wird wiederholt",
                error = error,
                details = mapOf(
                    "sender" to sender,
                    "parts_count" to parts.size,
                    "error_type" to error.javaClass.simpleName
                )
            )

            SnackbarManager.showError(
                "Fehler bei SMS von $sender: ${error.message}"
            )

            if (shouldRetry(error)) {
                scheduleRetry(sender, parts)
            }

        } catch (e: Exception) {
            LoggingManager.logError(
                component = "SmsForegroundService",
                action = "ERROR_HANDLER_FAILED",
                message = "Fehlerbehandlung fehlgeschlagen",
                error = e
            )
        }
    }

    private fun shouldRetry(error: Exception): Boolean {
        return when (error) {
            is IOException,         // Dies deckt auch SocketException ab
            is MessagingException -> true

            else -> false
        }
    }

    private fun scheduleRetry(sender: String, parts: List<SmsMessagePart>) {
        // Eindeutiger Key für diese SMS-Gruppe
        val retryKey = "${sender}_${parts.hashCode()}"

        // Aktueller Retry-Count für diese Nachricht
        val currentRetryCount = retryCounter.getOrDefault(retryKey, 0) + 1

        if (currentRetryCount > MAX_RETRIES) {
            LoggingManager.logError(
                component = "SmsForegroundService",
                action = "MAX_RETRIES_REACHED",
                message = "Maximale Anzahl an Wiederholungsversuchen erreicht, gebe auf",
                details = mapOf(
                    "sender" to sender,
                    "parts_count" to parts.size,
                    "retry_count" to currentRetryCount
                )
            )
            retryCounter.remove(retryKey)
            SnackbarManager.showError("SMS von $sender konnte nach $MAX_RETRIES Versuchen nicht verarbeitet werden")
            return
        }

        // Speichere aktuellen Retry-Count
        retryCounter[retryKey] = currentRetryCount

        // Exponentielles Backoff: 5s, 10s, 15s
        val delayMs = INITIAL_RETRY_DELAY * currentRetryCount

        LoggingManager.logInfo(
            component = "SmsForegroundService",
            action = "SCHEDULE_RETRY",
            message = "SMS-Verarbeitung wird wiederholt",
            details = mapOf(
                "sender" to sender,
                "retry_attempt" to currentRetryCount,
                "max_retries" to MAX_RETRIES,
                "delay_ms" to delayMs
            )
        )

        serviceScope.launch {
            delay(delayMs)
            try {
                processMessageGroup(sender, parts)
                // Bei Erfolg: Counter zurücksetzen
                retryCounter.remove(retryKey)
            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "SmsForegroundService",
                    action = "RETRY_FAILED",
                    message = "Retry-Versuch fehlgeschlagen",
                    error = e,
                    details = mapOf(
                        "sender" to sender,
                        "retry_attempt" to currentRetryCount
                    )
                )
            }
        }
    }

    private fun buildForwardedSmsMessage(
        sender: String,
        message: String,
        subscriptionId: Int = -1
    ): String {
        return buildString {
            append("Von: ").append(sender).append("\n")
            append("Zeit: ").append(getCurrentTimestamp()).append("\n")

            // Multi-SIM: Zeige über welche SIM die SMS empfangen wurde
            if (subscriptionId != -1) {
                append("SIM: Slot ").append(subscriptionId).append("\n")
            }

            append("Nachricht:\n").append(message)
            if (message.length > 160) {
                append("\n\n(Lange Nachricht, ${message.length} Zeichen)")
            }
        }
    }

    /**
     * Bestimmt die zu verwendende Subscription-ID basierend auf der Benutzereinstellung.
     * @param incomingSubscriptionId Die Subscription-ID der eingehenden SMS
     * @return Die zu verwendende Subscription-ID für ausgehende SMS
     */
    private fun determineTargetSubscriptionId(incomingSubscriptionId: Int): Int {
        val simSelectionMode = AppContainer.requirePrefsManager().getSimSelectionMode()
        val availableSims = PhoneSmsUtils.getAllSimInfo(applicationContext)

        return when (simSelectionMode) {
            SimSelectionMode.SAME_AS_INCOMING -> {
                // Verwende dieselbe SIM wie Eingang
                if (incomingSubscriptionId == -1) {
                    // Fallback: Verwende Standard-SMS-SIM
                    val defaultSim = PhoneSmsUtils.getDefaultSimIds(applicationContext)
                    LoggingManager.logWarning(
                        component = "SmsForegroundService",
                        action = "DETERMINE_SIM",
                        message = "Eingehende SIM unbekannt, verwende Standard-SIM",
                        details = mapOf("default_sms_sub_id" to (defaultSim?.first ?: -1))
                    )
                    defaultSim?.first ?: -1
                } else {
                    LoggingManager.logInfo(
                        component = "SmsForegroundService",
                        action = "DETERMINE_SIM",
                        message = "Using same SIM as incoming",
                        details = mapOf("subscription_id" to incomingSubscriptionId)
                    )
                    incomingSubscriptionId
                }
            }
            SimSelectionMode.ALWAYS_SIM_1 -> {
                val sim1 = availableSims.getOrNull(0)
                if (sim1 == null) {
                    LoggingManager.logWarning(
                        component = "SmsForegroundService",
                        action = "DETERMINE_SIM",
                        message = "SIM 1 nicht verfügbar, verwende Standard-SIM",
                        details = mapOf("available_sims" to availableSims.size)
                    )
                    SnackbarManager.showWarning("SIM 1 nicht verfügbar, verwende Standard-SIM")
                    -1 // Fallback: Standard-SIM
                } else {
                    LoggingManager.logInfo(
                        component = "SmsForegroundService",
                        action = "DETERMINE_SIM",
                        message = "Using SIM 1",
                        details = mapOf(
                            "subscription_id" to sim1.subscriptionId,
                            "slot_index" to sim1.slotIndex
                        )
                    )
                    sim1.subscriptionId
                }
            }
            SimSelectionMode.ALWAYS_SIM_2 -> {
                val sim2 = availableSims.getOrNull(1)
                if (sim2 == null) {
                    LoggingManager.logWarning(
                        component = "SmsForegroundService",
                        action = "DETERMINE_SIM",
                        message = "SIM 2 nicht verfügbar, verwende Fallback",
                        details = mapOf("available_sims" to availableSims.size)
                    )
                    SnackbarManager.showWarning("SIM 2 nicht verfügbar, verwende Standard-SIM")
                    // Fallback: Verwende SIM 1 wenn vorhanden, sonst Standard-SIM
                    availableSims.getOrNull(0)?.subscriptionId ?: -1
                } else {
                    LoggingManager.logInfo(
                        component = "SmsForegroundService",
                        action = "DETERMINE_SIM",
                        message = "Using SIM 2",
                        details = mapOf(
                            "subscription_id" to sim2.subscriptionId,
                            "slot_index" to sim2.slotIndex
                        )
                    )
                    sim2.subscriptionId
                }
            }
        }
    }

    /**
     * Sendet SMS mit spezifischer SIM-Auswahl.
     * @param targetNumber Die Zieltelefonnummer
     * @param message Die zu sendende Nachricht
     * @param subscriptionId Die zu verwendende Subscription-ID
     */
    private fun forwardSmsWithSubscription(
        targetNumber: String,
        message: String,
        subscriptionId: Int
    ) {
        try {
            // Max 10 Parts × 153 Zeichen (GSM-7) = 1530 Zeichen
            val maxSmsLength = 1530
            if (message.length > maxSmsLength) {
                LoggingManager.logWarning(
                    component = "SmsForegroundService",
                    action = "FORWARD_SMS",
                    message = "Nachricht zu lang für Weiterleitung",
                    details = mapOf(
                        "length" to message.length,
                        "max_length" to maxSmsLength,
                        "subscription_id" to subscriptionId
                    )
                )
                SnackbarManager.showWarning("Nachricht zu lang für SMS-Weiterleitung (max. $maxSmsLength Zeichen)")
                return
            }

            PhoneSmsUtils.sendSmsWithSubscription(
                applicationContext,
                targetNumber,
                message,
                subscriptionId
            )

            LoggingManager.logInfo(
                component = "SmsForegroundService",
                action = "FORWARD_SMS",
                message = "SMS erfolgreich weitergeleitet",
                details = mapOf(
                    "target" to targetNumber,
                    "length" to message.length,
                    "subscription_id" to subscriptionId,
                    "is_multipart" to (message.length > 160)
                )
            )

            updateServiceStatus()

        } catch (e: Exception) {
            LoggingManager.logError(
                component = "SmsForegroundService",
                action = "FORWARD_SMS_ERROR",
                message = "Fehler bei SMS-Weiterleitung",
                error = e,
                details = mapOf(
                    "target" to targetNumber,
                    "message_length" to message.length,
                    "subscription_id" to subscriptionId
                )
            )
            throw e
        }
    }

    private val SmsMessage.messageRef: Int
        get() = try {
            val field = SmsMessage::class.java.getDeclaredField("mMessageRef")
            field.isAccessible = true
            field.getInt(this)
        } catch (e: Exception) {
            0
        }

    private suspend fun handleEmailForwarding(sender: String, messageBody: String) {
        // Nutze structured concurrency - kein neuer serviceScope.launch!
        // Exceptions werden automatisch an Caller (processMessageGroup) propagiert
        withWakeLock(30 * 1000L) { // 30 Sekunden für Email-Versand
            try {
                val emailAddresses = prefsManager.getEmailAddresses()
                if (emailAddresses.isEmpty()) {
                    LoggingManager.logWarning(
                        component = "SmsForegroundService",
                        action = "EMAIL_FORWARD",
                        message = "Keine Email-Adressen konfiguriert"
                    )
                    return@withWakeLock
                }

                val host = prefsManager.getSmtpHost()
                val port = prefsManager.getSmtpPort()
                val username = prefsManager.getSmtpUsername()
                val password = prefsManager.getSmtpPassword()

                if (host.isEmpty() || username.isEmpty() || password.isEmpty()) {
                    LoggingManager.logWarning(
                        component = "SmsForegroundService",
                        action = "EMAIL_FORWARD",
                        message = "Unvollständige SMTP-Konfiguration",
                        details = mapOf(
                            "has_host" to host.isNotEmpty(),
                            "has_username" to username.isNotEmpty(),
                            "has_credentials" to password.isNotEmpty()
                        )
                    )
                    return@withWakeLock
                }

                val emailSender = EmailSender(host, port, username, password)
                val subject = "Neue SMS von $sender"
                val body = buildEmailBody(sender, messageBody)

                when (val result = emailSender.sendEmail(emailAddresses, subject, body)) {
                    is EmailResult.Success -> {
                        LoggingManager.logInfo(
                            component = "SmsForegroundService",
                            action = "EMAIL_FORWARD",
                            message = "SMS erfolgreich per Email weitergeleitet",
                            details = mapOf(
                                "sender" to sender,
                                "recipients" to emailAddresses.size,
                                "smtp_host" to host
                            )
                        )
                        SnackbarManager.showSuccess("SMS per Email weitergeleitet")
                        updateServiceStatus()
                    }

                    is EmailResult.Error -> handleEmailError(
                        result.message,
                        sender,
                        emailAddresses,
                        host
                    )
                }
            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "SmsForegroundService",
                    action = "EMAIL_FORWARD_ERROR",
                    message = "Unerwarteter Fehler bei Email-Weiterleitung",
                    error = e,
                    details = mapOf(
                        "sender" to sender,
                        "message_length" to messageBody.length
                    )
                )
                SnackbarManager.showError("Fehler bei der Email-Weiterleitung")
                // Re-throw um in processMessageGroup error handling zu triggern
                throw e
            }
        }
    }

    private fun handleEmailError(
        errorMessage: String,
        sender: String,
        recipients: List<String>,
        host: String
    ) {
        LoggingManager.logError(
            component = "SmsForegroundService",
            action = "EMAIL_FORWARD",
            message = "Email-Weiterleitung fehlgeschlagen",
            details = mapOf(
                "error" to errorMessage,
                "smtp_host" to host,
                "recipients" to recipients.size,
                "sender" to sender
            )
        )
        SnackbarManager.showError("Email-Weiterleitung fehlgeschlagen: $errorMessage")
    }

    private fun buildEmailBody(sender: String, messageBody: String): String {
        return buildString {
            append("SMS Weiterleitung\n\n")
            append("Absender: $sender\n")
            append("Zeitpunkt: ${getCurrentTimestamp()}\n\n")
            append("Nachricht:\n")
            append(messageBody)
            append("\n\nDiese E-Mail wurde automatisch durch den SMS Forwarder generiert.")
        }
    }

    private fun updateServiceStatus() {
        val status = buildServiceStatus(prefsManager)
        updateNotification(status)
    }

    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
            .format(Date())
    }

    private fun createNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TEL/SMS Forwarder")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setSmallIcon(R.drawable.logofwd2)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(message: String) {
        if (!isRunning) return

        try {
            currentNotificationText = message
            val notification = createNotification(message)
            notificationManager.notify(NOTIFICATION_ID, notification)

            LoggingManager.logInfo(
                component = "SmsForegroundService",
                action = "UPDATE_NOTIFICATION",
                message = "Notification aktualisiert",
                details = mapOf(
                    "text" to message,
                    "success" to true
                )
            )
        } catch (e: Exception) {
            LoggingManager.logError(
                component = "SmsForegroundService",
                action = "UPDATE_NOTIFICATION",
                message = "Fehler beim Aktualisieren der Notification",
                error = e
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundService() {
        isRunning = true
        val prefs = SharedPreferencesManager(this)
        val initialStatus = buildServiceStatus(prefs)
        currentNotificationText = initialStatus
        val notification = createNotification(initialStatus)
        startForeground(NOTIFICATION_ID, notification)

    }

    private fun buildServiceStatus(prefs: SharedPreferencesManager): String {
        return buildString {
            if (!prefs.isForwardingActive() && !prefs.isForwardSmsToEmail()) {
                append(DEFAULT_NOTIFICATION_TEXT)  // Statt "Keine Weiterleitung aktiv"
            } else {
                if (prefs.isForwardingActive()) {
                    append("SMS-Weiterleitung aktiv")
                    prefs.getSelectedPhoneNumber().let { number ->
                        if (number.isNotEmpty()) {
                            append(" zu $number")
                        }
                    }
                }
                if (prefs.isForwardSmsToEmail()) {
                    if (prefs.isForwardingActive()) append("\n")
                    append("Email-Weiterleitung aktiv")
                    val emailCount = prefs.getEmailAddresses().size
                    append(" an $emailCount Email(s)")
                }
            }
        }
    }

    override fun onDestroy() {
        try {
            // Cancelle den Scope ohne zu warten (non-blocking)
            // cancel() beendet automatisch alle child coroutines
            serviceScope.cancel()

            LoggingManager.logInfo(
                component = "SmsForegroundService",
                action = "DESTROY",
                message = "Foreground Service wurde beendet, Coroutines werden abgebrochen"
            )

        } catch (e: Exception) {
            LoggingManager.logError(
                component = "SmsForegroundService",
                action = "DESTROY_ERROR",
                message = "Fehler beim Beenden des Services",
                error = e
            )
        } finally {
            isRunning = false
            super.onDestroy()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Prüfe ob Service weiterlaufen soll
        if (!SharedPreferencesManager(this).getKeepForwardingOnExit()) {
            stopSelf()
        }
        // Wenn keepForwardingOnExit = true, läuft der Service einfach weiter
        // START_STICKY sorgt dafür, dass das System ihn bei Bedarf neu startet
    }
}

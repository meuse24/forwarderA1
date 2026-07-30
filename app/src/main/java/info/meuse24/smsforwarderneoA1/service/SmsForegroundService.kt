package info.meuse24.smsforwarderneoA1.service

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.provider.Telephony
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import info.meuse24.smsforwarderneoA1.AppContainer
import info.meuse24.smsforwarderneoA1.BuildConfig
import info.meuse24.smsforwarderneoA1.ForwardSmsPreparation
import info.meuse24.smsforwarderneoA1.LoggingManager
import info.meuse24.smsforwarderneoA1.MainActivity
import info.meuse24.smsforwarderneoA1.PhoneNumberValidator
import info.meuse24.smsforwarderneoA1.PhoneSmsUtils
import info.meuse24.smsforwarderneoA1.R
import info.meuse24.smsforwarderneoA1.SnackbarManager
import info.meuse24.smsforwarderneoA1.data.local.EmailQueueStore
import info.meuse24.smsforwarderneoA1.data.local.EmailQueueUpdate
import info.meuse24.smsforwarderneoA1.data.local.ForwardingQueueStore
import info.meuse24.smsforwarderneoA1.data.local.QueueUpdate
import info.meuse24.smsforwarderneoA1.data.local.SharedPreferencesManager
import info.meuse24.smsforwarderneoA1.domain.model.EmailBodyComposer
import info.meuse24.smsforwarderneoA1.domain.model.EmailDeliveryReducer
import info.meuse24.smsforwarderneoA1.domain.model.EmailDeliveryState
import info.meuse24.smsforwarderneoA1.domain.model.EmailDispatchBudget
import info.meuse24.smsforwarderneoA1.domain.model.EmailFailure
import info.meuse24.smsforwarderneoA1.domain.model.EmailFailureKind
import info.meuse24.smsforwarderneoA1.domain.model.EmailForwardingJob
import info.meuse24.smsforwarderneoA1.domain.model.EmailQueueRetentionPolicy
import info.meuse24.smsforwarderneoA1.domain.model.ForwardingQueueRetentionPolicy
import info.meuse24.smsforwarderneoA1.domain.model.ForwardingOperation
import info.meuse24.smsforwarderneoA1.domain.model.ForwardingState
import info.meuse24.smsforwarderneoA1.domain.model.ForwardingVerification
import info.meuse24.smsforwarderneoA1.domain.model.LoopProtectionPolicy
import info.meuse24.smsforwarderneoA1.domain.model.SimInfo
import info.meuse24.smsforwarderneoA1.domain.model.SimSelectionMode
import info.meuse24.smsforwarderneoA1.domain.model.SmsDeliveryReducer
import info.meuse24.smsforwarderneoA1.domain.model.SmsForwardingComposer
import info.meuse24.smsforwarderneoA1.domain.model.SmsMessagePart
import info.meuse24.smsforwarderneoA1.util.MmiCodeMasker
import info.meuse24.smsforwarderneoA1.util.email.EmailSendOutcome
import info.meuse24.smsforwarderneoA1.util.email.EmailSender
import info.meuse24.smsforwarderneoA1.util.email.SmtpConfig
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class SmsForegroundService : Service() {
    private val prefsManager: SharedPreferencesManager by lazy {
        AppContainer.requirePrefsManager()
    }
    private val queue: ForwardingQueueStore by lazy {
        AppContainer.requireForwardingQueue()
    }
    private val emailQueue: EmailQueueStore by lazy {
        AppContainer.requireEmailQueue()
    }
    private var currentNotificationText = ""
    private lateinit var defaultNotificationText: String

    /**
     * Ein `CoroutineExceptionHandler` haelt Fehler ausserhalb der inneren try-Bloecke vom
     * Prozess fern. Ohne ihn reisst eine einzige unbehandelte Exception den ganzen Dienst mit.
     */
    private val serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, error ->
            LoggingManager.logError(
                component = "SmsForegroundService",
                action = "UNCAUGHT_COROUTINE_ERROR",
                message = "Unbehandelte Exception in einer Service-Coroutine",
                error = error
            )
        }
    )
    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: throw IllegalStateException("NotificationManager nicht verfügbar")
    }
    private var wakeLock: PowerManager.WakeLock? = null
    private val wakeLockMutex = Mutex()
    private val wakeLockTimeout = 5 * 60 * 1000L // 5 Minuten Maximum
    private val wakeLockTag = "${BuildConfig.APPLICATION_ID}:ForegroundService"
    private val emailWakeLockTag = "${BuildConfig.APPLICATION_ID}:EmailDispatch"

    /**
     * Serialisiert die E-Mail-Zustellung. **Getrennt** von [wakeLockMutex]: Dieser serialisiert den
     * SMS-Empfang, und ein haengender SMTP-Server wuerde darueber jede eintreffende SMS aufhalten.
     */
    private val emailDispatchMutex = Mutex()

    /** Wird beim Eintreffen einer Netzverbindung angemeldet, um faellige Auftraege sofort zu senden. */
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    /** Verhindert, dass derselbe Vorgang mehrfach parallel auf seinen Neuversuch wartet. */
    private val scheduledDispatches = ConcurrentHashMap<String, Job>()

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "MY_CHANNEL_ID"

        const val ACTION_START = "START_SERVICE"
        const val ACTION_PROCESS_SMS = "PROCESS_SMS"
        const val ACTION_UPDATE_NOTIFICATION = "UPDATE_NOTIFICATION"

        /** Anstoss von aussen, die Queue sofort zu pruefen (z. B. nach einem Sende-Callback). */
        const val ACTION_PROCESS_QUEUE = "PROCESS_QUEUE"

        /** Takt des Ablauf-Scans. Deckt beide Kanaele ab. */
        private const val QUEUE_SCAN_INTERVAL_MILLIS = 5 * 60 * 1000L


        @Volatile
        private var isRunning = false

        /**
         * Der erste Scan nach einem Prozessstart darf vorgefundene `ATTEMPTING`-Eintraege nach
         * `UNKNOWN` ueberfuehren - zu diesem Zeitpunkt kann kein Sendeversuch mehr laufen.
         * Spaetere Durchlaeufe duerfen das nicht, sie wuerden laufende Versuche zerstoeren.
         */
        private val firstScanAfterProcessStart = AtomicBoolean(true)

        /**
         * Gegenstueck fuer den E-Mail-Kanal. Ein vorgefundenes `ATTEMPTING` geht dort **zurueck
         * nach `QUEUED`** statt nach `UNKNOWN`: Eine E-Mail kostet nichts, der Verlust einer
         * Weiterleitung ist der eigentliche Schadensfall.
         */
        private val emailScanAfterProcessStart = AtomicBoolean(true)

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

        /**
         * Bittet den Dienst, die Queue sofort zu pruefen. Schlaegt der Start fehl - etwa weil der
         * Dienst nicht laeuft und der Hintergrundstart verwehrt wird -, uebernimmt der naechste
         * regulaere Scan; deshalb wird hier nur protokolliert.
         */
        fun requestQueueScan(context: Context) {
            try {
                val intent = Intent(context, SmsForegroundService::class.java).apply {
                    action = ACTION_PROCESS_QUEUE
                }
                context.startForegroundService(intent)
            } catch (e: Exception) {
                LoggingManager.logWarning(
                    component = "SmsForegroundService",
                    action = "QUEUE_SCAN_REQUEST_FAILED",
                    message = "Queue-Pruefung konnte nicht angestossen werden",
                    details = mapOf("error" to (e.message ?: e.javaClass.simpleName))
                )
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        try {
            setupService()
            startQueueMaintenance()
            registerNetworkCallback()
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

    /**
     * Startet den Vordergrunddienst.
     *
     * Ohne `POST_NOTIFICATIONS` laeuft der Dienst **weiter**: Die Plattform verlangt die
     * Berechtigung fuer einen Foreground Service nicht, der Dienst bleibt im Task Manager
     * sichtbar, nur nicht im Benachrichtigungsbereich. Sich hier selbst zu stoppen hiesse, einen
     * Totalausfall zu erzeugen, den Android gar nicht erzwingt.
     */
    private fun setupService() {
        defaultNotificationText = getString(R.string.notification_running_long)
        if (currentNotificationText.isBlank()) {
            currentNotificationText = getString(R.string.notification_running_short)
        }

        val notificationsSuppressed = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED

        runCatching { prefsManager.setNotificationsSuppressed(notificationsSuppressed) }
        if (notificationsSuppressed) {
            LoggingManager.logWarning(
                component = "SmsForegroundService",
                action = "NOTIFICATION_SUPPRESSED",
                message = "Statusanzeige unterdrueckt - Weiterleitung laeuft weiter",
                details = mapOf("sdk_version" to Build.VERSION.SDK_INT)
            )
        }

        val notification = createNotification(defaultNotificationText)
        startForeground(NOTIFICATION_ID, notification)
        isRunning = true

        LoggingManager.logInfo(
            component = "SmsForegroundService",
            action = "START",
            message = "Service started successfully",
            details = mapOf(
                "sdk_version" to Build.VERSION.SDK_INT,
                "notifications_suppressed" to notificationsSuppressed
            )
        )
    }

    /**
     * Zeitlimit der Plattform fuer diesen Foreground-Service-Typ.
     *
     * Fuer `specialUse` ist heute kein Limit dokumentiert. Die Methode bleibt trotzdem
     * implementiert: Sie verwandelt eine kuenftige Plattformverschaerfung von einem Absturz
     * (`RemoteServiceException`, wenn der Dienst sich nicht selbst beendet) in eine
     * kontrollierte, sichtbare Meldung.
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onTimeout(startId: Int, fgsType: Int) {
        LoggingManager.logError(
            component = "SmsForegroundService",
            action = "FGS_TIMEOUT",
            message = "Foreground Service durch Zeitlimit beendet",
            details = mapOf("start_id" to startId, "fgs_type" to fgsType)
        )
        runCatching { prefsManager.recordServiceTimeout(System.currentTimeMillis()) }
            .onSuccess { persisted ->
                if (!persisted) {
                    LoggingManager.logError(
                        component = "SmsForegroundService",
                        action = "FGS_TIMEOUT",
                        message = "Zeitlimit-Hinweis nicht haltbar geschrieben - nur im Protokoll"
                    )
                }
            }
        stopSelf()
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
                ACTION_START -> {
                    if (!isRunning) {
                        startForegroundService()
                    }
                }

                ACTION_PROCESS_SMS -> {
                    if (!isRunning) {
                        startForegroundService()
                    }

                    intent.extras?.let { extras ->
                        serviceScope.launch {
                            processSmsData(extras)
                        }
                    }
                }

                ACTION_PROCESS_QUEUE -> {
                    if (!isRunning) {
                        startForegroundService()
                    }
                    serviceScope.launch {
                        withContext(Dispatchers.IO) {
                            scanQueue()
                            processEmailQueue()
                        }
                    }
                }

                ACTION_UPDATE_NOTIFICATION -> {
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
                    serviceScope.launch {
                        withContext(Dispatchers.IO) {
                            scanQueue()
                            processEmailQueue()
                        }
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
            // Kein stopSelf(): Ein fehlgeschlagenes Kommando ist kein Grund, die Weiterleitung
            // insgesamt aufzugeben.
            return START_STICKY
        }
    }

    // --- Empfang ------------------------------------------------------------------------------

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

                // Eigene ID je Intent, damit unabhaengige einteilige Nachrichten nicht
                // zusammengefasst werden.
                val intentId = UUID.randomUUID().toString()

                val messageParts = messages.mapIndexedNotNull { index, smsMessage ->
                    // Einzelne unbrauchbare Teile werden uebersprungen, nicht der ganze Intent
                    // verworfen - sonst ginge bei einer mehrteiligen Nachricht alles verloren.
                    val sender = smsMessage?.originatingAddress ?: return@mapIndexedNotNull null
                    val body = smsMessage.messageBody?.takeIf { it.isNotEmpty() }
                        ?: return@mapIndexedNotNull null
                    SmsMessagePart(
                        body = body,
                        timestamp = smsMessage.timestampMillis,
                        referenceNumber = intentId.hashCode(),
                        sequencePosition = index,
                        totalParts = messages.size,
                        sender = sender,
                        subscriptionId = subscriptionId,
                        intentId = intentId
                    )
                }.takeIf { it.isNotEmpty() } ?: run {
                    LoggingManager.logWarning(
                        component = "SmsForegroundService",
                        action = "PROCESS_SMS",
                        message = "Keine gültigen Nachrichtenteile extrahiert"
                    )
                    return@withWakeLock
                }

                val groups = SmsForwardingComposer.group(messageParts)
                groups.forEach { group -> processMessageGroup(group) }

                LoggingManager.logInfo(
                    component = "SmsForegroundService",
                    action = "PROCESS_SMS",
                    message = "SMS-Verarbeitung abgeschlossen",
                    details = mapOf(
                        "messages_count" to messages.size,
                        "groups_count" to groups.size,
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
                SnackbarManager.showError(getString(R.string.snackbar_sms_processing_error, e.message ?: ""))
            }
        }
    }

    /**
     * Verarbeitet eine zusammengehoerige Nachricht auf beiden Kanaelen.
     *
     * `supervisorScope` statt `coroutineScope`: Die Kanaele sind fachlich unabhaengig. Unter
     * Structured Concurrency wuerde ein sofort scheiternder SMTP-Zweig den noch suspendierten
     * SMS-Zweig mit abbrechen - ein unerwuenschter Nebeneffekt, kein gewolltes Verhalten.
     */
    private suspend fun processMessageGroup(group: SmsForwardingComposer.Group) {
        LoggingManager.logDebug(
            component = "SmsForegroundService",
            action = "PROCESS_MESSAGE_GROUP",
            message = "Verarbeite SMS-Gruppe",
            details = mapOf(
                "sender" to MmiCodeMasker.maskNumber(group.sender),
                "parts_count" to group.parts.size,
                "total_length" to group.text.length,
                "subscription_id" to group.subscriptionId
            )
        )

        supervisorScope {
            if (prefsManager.isForwardingActive()) {
                launch {
                    try {
                        withContext(Dispatchers.IO) { forwardViaSms(group) }
                    } catch (e: Exception) {
                        LoggingManager.logError(
                            component = "SmsForegroundService",
                            action = "FORWARD_SMS_ERROR",
                            message = "Fehler im SMS-Zweig",
                            error = e,
                            details = mapOf("sender" to MmiCodeMasker.maskNumber(group.sender))
                        )
                    }
                }
            }

            if (prefsManager.isForwardSmsToEmail()) {
                launch {
                    try {
                        withContext(Dispatchers.IO) { enqueueEmailForwarding(group) }
                    } catch (e: Exception) {
                        LoggingManager.logError(
                            component = "SmsForegroundService",
                            action = "ENQUEUE_EMAIL_ERROR",
                            message = "Fehler im E-Mail-Zweig",
                            error = e,
                            details = mapOf("sender" to MmiCodeMasker.maskNumber(group.sender))
                        )
                    }
                }
            }
        }
    }

    // --- SMS-Kanal ----------------------------------------------------------------------------

    /**
     * Reiht die Weiterleitung persistent ein und stoesst den Versand an.
     *
     * Ohne erfolgreich geschriebenen Queue-Eintrag wird **nicht** gesendet: Ein Versand, von dem
     * nach einem Prozessverlust niemand mehr weiss, ist schlimmer als ein unterbliebener.
     */
    private fun forwardViaSms(group: SmsForwardingComposer.Group) {
        val targetNumber = prefsManager.getSelectedPhoneNumber()
        val validator = PhoneNumberValidator(applicationContext)
        // Einmalige Abfrage je Nachricht: getAllSimInfo() ist ein Binder-Call.
        val sims = PhoneSmsUtils.getAllSimInfo(applicationContext)
        val ownNumbers = sims.mapNotNull { it.phoneNumber } + prefsManager.getSimPhoneNumbers().values

        val decision = LoopProtectionPolicy.decide(group.sender, targetNumber, ownNumbers) { a, b ->
            validator.areSameNumber(a, b)
        }
        when (decision) {
            LoopProtectionPolicy.Decision.BLOCKED_SENDER_IS_TARGET -> {
                LoggingManager.logWarning(
                    component = "SmsForegroundService",
                    action = "LOOP_PROTECTION_SENDER",
                    message = "Weiterleitung gestoppt: Absender entspricht Zielrufnummer",
                    details = mapOf("sender" to MmiCodeMasker.maskNumber(group.sender), "target" to MmiCodeMasker.maskNumber(targetNumber))
                )
                return
            }

            LoopProtectionPolicy.Decision.BLOCKED_TARGET_IS_OWN_SIM -> {
                LoggingManager.logError(
                    component = "SmsForegroundService",
                    action = "LOOP_PROTECTION_CRITICAL",
                    message = "Weiterleitung gestoppt: Zielnummer ist eine eigene SIM-Karte!",
                    details = mapOf(
                        "target" to MmiCodeMasker.maskNumber(targetNumber),
                        "own_numbers_count" to ownNumbers.size,
                        "sender" to MmiCodeMasker.maskNumber(group.sender)
                    )
                )
                SnackbarManager.showError(getString(R.string.snackbar_loop_protection_own_sim))
                return
            }

            LoopProtectionPolicy.Decision.FORWARD -> Unit
        }

        val incomingSlot = sims.firstOrNull { it.subscriptionId == group.subscriptionId }?.slotIndex
        val text = SmsForwardingComposer.compose(
            sender = group.sender,
            timestamp = compactTimestamp(),
            slotIndex = incomingSlot,
            body = group.text
        )
        // Laufende Vorgaenge werden nie verdraengt (siehe ForwardingQueueRetentionPolicy). Ist
        // die Obergrenze erreicht, wird gar nicht erst eingereiht: Ein Vermerk *in* der Queue
        // waere der naechste Kandidat der Aufbewahrungsregel und wuerde sich selbst loeschen.
        // Der Verlust wird deshalb ausserhalb der Queue dauerhaft vermerkt.
        if (queue.activeCount() >= ForwardingQueueRetentionPolicy.MAX_ENTRIES) {
            val recorded = runCatching { prefsManager.recordDroppedForwarding() }.getOrDefault(false)
            LoggingManager.logError(
                component = "SmsForegroundService",
                action = "QUEUE_FULL",
                message = if (recorded) {
                    "Warteschlange voll - kein Sendeversuch, Verlust dauerhaft vermerkt"
                } else {
                    "Warteschlange voll - kein Sendeversuch, Verlust NUR im Protokoll"
                },
                details = mapOf(
                    "sender" to MmiCodeMasker.maskNumber(group.sender),
                    "max_entries" to ForwardingQueueRetentionPolicy.MAX_ENTRIES,
                    "warning_persisted" to recorded
                )
            )
            SnackbarManager.showError(getString(R.string.warning_queue_full))
            return
        }

        val now = System.currentTimeMillis()
        val operation = SmsDeliveryReducer.queue(
            id = UUID.randomUUID().toString(),
            now = now,
            sender = group.sender,
            targetNumber = targetNumber,
            text = text,
            subscriptionId = determineTargetSubscriptionId(group.subscriptionId, sims),
            expectedParts = 1
        )

        if (!queue.enqueue(operation)) {
            LoggingManager.logError(
                component = "SmsForegroundService",
                action = "QUEUE_WRITE_FAILED",
                message = "Vorgang konnte nicht persistiert werden - kein Sendeversuch",
                details = mapOf("sender" to MmiCodeMasker.maskNumber(group.sender))
            )
            SnackbarManager.showError(getString(R.string.warning_queue_write_failed))
            return
        }

        dispatchOperation(operation)
    }

    /**
     * Fuehrt einen Sendeversuch aus und schreibt jeden Uebergang **vor** seinem Seiteneffekt.
     *
     * Reihenfolge und Begruendung:
     * 1. Alle Vorbedingungen vor `ATTEMPTING` - danach ist eine Exception nicht mehr eindeutig
     *    einer Vorbedingung zuzuordnen.
     * 2. `ATTEMPTING` synchron schreiben, und zwar als **Inbesitznahme**: Nur wer den Vorgang aus
     *    einem sendefaelligen Zustand uebernimmt, darf senden. Schlaegt das Schreiben fehl,
     *    unterbleibt der Sendeaufruf; der Vorgang bleibt `QUEUED` und damit nachweislich
     *    ungesendet. War ein anderer Durchlauf schneller - etwa der Queue-Scan parallel zum
     *    Versand direkt nach dem Einreihen -, sendet nur dieser.
     * 3. Erst danach senden.
     */
    private fun dispatchOperation(operation: ForwardingOperation) {
        scheduledDispatches.remove(operation.id)
        val preparation = PhoneSmsUtils.prepareForwardSms(
            applicationContext,
            operation.targetNumber,
            operation.text,
            operation.subscriptionId
        )

        if (preparation is ForwardSmsPreparation.Rejected) {
            val rejection = queue.update(operation.id) {
                SmsDeliveryReducer.onPreconditionRejected(it, System.currentTimeMillis(), preparation.reason)
            }
            if (rejection is QueueUpdate.Rejected) {
                // Ein anderer Durchlauf sendet bereits; dessen Versand darf nicht durch das
                // Pruefergebnis dieses veralteten Durchlaufs fuer gescheitert erklaert werden.
                LoggingManager.logWarning(
                    component = "SmsForegroundService",
                    action = "SEND_PRECONDITION_STALE",
                    message = "Vorbedingung verletzt, Vorgang aber bereits in Arbeit - kein Zustandswechsel",
                    details = mapOf(
                        "operation_id" to operation.id,
                        "reason" to preparation.reason,
                        "state" to rejection.operation.state.name
                    )
                )
                return
            }
            LoggingManager.logError(
                component = "SmsForegroundService",
                action = "SEND_PRECONDITION_FAILED",
                message = "Sendevorbedingung verletzt",
                details = mapOf("operation_id" to operation.id, "reason" to preparation.reason)
            )
            return
        }

        val prepared = (preparation as ForwardSmsPreparation.Ready).prepared
        val claim = queue.update(operation.id) {
            SmsDeliveryReducer.onAttemptStart(it, System.currentTimeMillis(), prepared.partCount)
        }
        val attempting = when (claim) {
            is QueueUpdate.Applied -> claim.operation

            is QueueUpdate.Rejected -> {
                LoggingManager.logInfo(
                    component = "SmsForegroundService",
                    action = "DISPATCH_SKIPPED",
                    message = "Vorgang wird bereits von einem anderen Durchlauf gesendet",
                    details = mapOf(
                        "operation_id" to operation.id,
                        "state" to claim.operation.state.name
                    )
                )
                return
            }

            QueueUpdate.NotStored -> {
                LoggingManager.logError(
                    component = "SmsForegroundService",
                    action = "ATTEMPTING_COMMIT_FAILED",
                    message = "Zustand ATTEMPTING nicht haltbar geschrieben - Sendeaufruf unterbleibt",
                    details = mapOf("operation_id" to operation.id)
                )
                return
            }
        }

        try {
            PhoneSmsUtils.dispatchForwardSms(applicationContext, prepared, attempting.id, attempting.attempt)
            queue.update(operation.id) {
                SmsDeliveryReducer.onHandedOver(it, System.currentTimeMillis())
            }
            updateServiceStatus()
        } catch (e: Exception) {
            val result = queue.update(operation.id) {
                SmsDeliveryReducer.onDispatchException(it, System.currentTimeMillis(), e)
            }
            LoggingManager.logError(
                component = "SmsForegroundService",
                action = "DISPATCH_FAILED",
                message = "Sendeaufruf fehlgeschlagen",
                error = e,
                details = mapOf(
                    "operation_id" to operation.id,
                    "new_state" to ((result as? QueueUpdate.Applied)?.operation?.state?.name ?: "unchanged")
                )
            )
        }
    }

    /**
     * Ablauf-Scan. Laeuft im dauerhaft aktiven Dienst statt in WorkManager: Expedited Work
     * degradiert bei erschoepftem Kontingent zu verzoegerbarer Arbeit.
     */
    private fun scanQueue() {
        val now = System.currentTimeMillis()

        if (firstScanAfterProcessStart.compareAndSet(true, false)) {
            queue.updateAll { SmsDeliveryReducer.onProcessRestart(it, now) }
        }
        queue.updateAll { SmsDeliveryReducer.onExpiryScan(it, now) }

        queue.all().forEach { operation ->
            when {
                SmsDeliveryReducer.isDispatchDue(operation, now) -> dispatchOperation(operation)
                operation.state == ForwardingState.RETRY -> scheduleDispatch(operation, now)
                else -> Unit
            }
        }
    }

    /** Wartet den Backoff eines faelligen Neuversuchs ab, ohne den Scan-Takt zu blockieren. */
    private fun scheduleDispatch(operation: ForwardingOperation, now: Long) {
        if (scheduledDispatches[operation.id]?.isActive == true) return
        val waitMillis = ((operation.nextAttemptAtMillis ?: now) - now).coerceAtLeast(0)
        val job = serviceScope.launch {
            delay(waitMillis)
            withContext(Dispatchers.IO) {
                queue.all()
                    .firstOrNull { it.id == operation.id }
                    ?.takeIf { SmsDeliveryReducer.isDispatchDue(it, System.currentTimeMillis()) }
                    ?.let { dispatchOperation(it) }
            }
        }
        scheduledDispatches[operation.id] = job
    }

    private fun startQueueMaintenance() {
        serviceScope.launch {
            while (isActive) {
                withContext(Dispatchers.IO) {
                    runCatching { scanQueue() }.onFailure { error ->
                        LoggingManager.logError(
                            component = "SmsForegroundService",
                            action = "QUEUE_SCAN_ERROR",
                            message = "Ablauf-Scan fehlgeschlagen",
                            error = error
                        )
                    }
                    runCatching { processEmailQueue() }.onFailure { error ->
                        LoggingManager.logError(
                            component = "SmsForegroundService",
                            action = "EMAIL_SCAN_ERROR",
                            message = "E-Mail-Durchlauf fehlgeschlagen",
                            error = error
                        )
                    }
                }
                delay(QUEUE_SCAN_INTERVAL_MILLIS)
            }
        }
    }

    /**
     * Meldet sich fuer die Standardverbindung an und sendet faellige Auftraege, sobald wieder Netz
     * da ist.
     *
     * Der 5-Minuten-Takt bleibt die Rueckfallebene; dies ist nur eine Beschleunigung. Ein
     * `JobScheduler`-Constraint waere ein zweiter Planer mit eigenem Lebenszyklus und eigenem
     * Kontingent - eine zusaetzliche Fehlerquelle ohne Gegenwert, solange der Dienst ohnehin laeuft.
     */
    private fun registerNetworkCallback() {
        val manager = getSystemService(ConnectivityManager::class.java) ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                serviceScope.launch {
                    runCatching { processEmailQueue() }.onFailure { error ->
                        LoggingManager.logError(
                            component = "SmsForegroundService",
                            action = "EMAIL_SCAN_ERROR",
                            message = "E-Mail-Durchlauf nach Netzwiederkehr fehlgeschlagen",
                            error = error
                        )
                    }
                }
            }
        }
        runCatching { manager.registerDefaultNetworkCallback(callback) }
            .onSuccess { networkCallback = callback }
            .onFailure { error ->
                LoggingManager.logWarning(
                    component = "SmsForegroundService",
                    action = "NETWORK_CALLBACK_FAILED",
                    message = "Netzbeobachtung nicht moeglich - Wiederholungen laufen ueber den Scan-Takt",
                    details = mapOf("error" to (error.message ?: error.javaClass.simpleName))
                )
            }
    }

    /**
     * Bestimmt die Subscription-ID fuer den Versand.
     * @param sims bereits ermittelte SIM-Liste, damit der Binder nicht erneut befragt wird
     */
    private fun determineTargetSubscriptionId(incomingSubscriptionId: Int, sims: List<SimInfo>): Int {
        return when (prefsManager.getSimSelectionMode()) {
            SimSelectionMode.SAME_AS_INCOMING -> {
                if (incomingSubscriptionId == -1) {
                    val defaultSim = PhoneSmsUtils.getDefaultSimIds(applicationContext)
                    LoggingManager.logWarning(
                        component = "SmsForegroundService",
                        action = "DETERMINE_SIM",
                        message = "Eingehende SIM unbekannt, verwende Standard-SIM",
                        details = mapOf("default_sms_sub_id" to (defaultSim?.first ?: -1))
                    )
                    defaultSim?.first ?: -1
                } else {
                    incomingSubscriptionId
                }
            }

            SimSelectionMode.ALWAYS_SIM_1 -> sims.getOrNull(0)?.subscriptionId ?: run {
                LoggingManager.logWarning(
                    component = "SmsForegroundService",
                    action = "DETERMINE_SIM",
                    message = "SIM 1 nicht verfügbar, verwende Standard-SIM",
                    details = mapOf("available_sims" to sims.size)
                )
                SnackbarManager.showWarning(getString(R.string.snackbar_sim1_unavailable))
                -1
            }

            SimSelectionMode.ALWAYS_SIM_2 -> sims.getOrNull(1)?.subscriptionId ?: run {
                LoggingManager.logWarning(
                    component = "SmsForegroundService",
                    action = "DETERMINE_SIM",
                    message = "SIM 2 nicht verfügbar, verwende Fallback",
                    details = mapOf("available_sims" to sims.size)
                )
                SnackbarManager.showWarning(getString(R.string.snackbar_sim2_unavailable))
                sims.getOrNull(0)?.subscriptionId ?: -1
            }
        }
    }

    // --- E-Mail-Kanal -------------------------------------------------------------------------

    /**
     * Reiht die E-Mail-Weiterleitung persistent ein und stoesst den Versand **asynchron** an.
     *
     * Der Empfangspfad darf hier nicht auf das Netz warten: Er laeuft unter [wakeLockMutex], der
     * jede weitere eintreffende SMS serialisiert - ein haengender SMTP-Server wuerde sonst auch
     * den SMS-Kanal blockieren. Geschrieben wird deshalb nur der Auftrag; gesendet wird im
     * Queue-Durchlauf.
     */
    private fun enqueueEmailForwarding(group: SmsForwardingComposer.Group) {
        val recipients = prefsManager.getEmailAddresses()
        if (recipients.isEmpty()) {
            LoggingManager.logWarning(
                component = "SmsForegroundService",
                action = "EMAIL_ENQUEUE",
                message = "Keine Email-Adressen konfiguriert"
            )
            return
        }

        // Wie beim SMS-Kanal: Ist die Obergrenze erreicht, wird nicht eingereiht und der Verlust
        // ausserhalb der Queue vermerkt - ein Vermerk *in* der Queue waere der naechste Kandidat
        // der Aufbewahrungsregel und wuerde sich selbst loeschen.
        if (emailQueue.activeCount() >= EmailQueueRetentionPolicy.MAX_ENTRIES) {
            val recorded = runCatching { prefsManager.recordDroppedEmail() }.getOrDefault(false)
            LoggingManager.logError(
                component = "SmsForegroundService",
                action = "EMAIL_QUEUE_FULL",
                message = if (recorded) {
                    "E-Mail-Warteschlange voll - kein Versand, Verlust dauerhaft vermerkt"
                } else {
                    "E-Mail-Warteschlange voll - kein Versand, Verlust NUR im Protokoll"
                },
                details = mapOf(
                    "sender" to MmiCodeMasker.maskNumber(group.sender),
                    "max_entries" to EmailQueueRetentionPolicy.MAX_ENTRIES,
                    "warning_persisted" to recorded
                )
            )
            SnackbarManager.showError(getString(R.string.warning_email_queue_full))
            return
        }

        val now = System.currentTimeMillis()
        val job = EmailDeliveryReducer.queue(
            id = UUID.randomUUID().toString(),
            now = now,
            sender = group.sender,
            // Der Empfangszeitpunkt der SMS, nicht der Verarbeitungszeitpunkt.
            receivedAtMillis = group.receivedAtMillis.takeIf { it > 0 } ?: now,
            body = group.text,
            recipients = recipients
        )

        if (!emailQueue.enqueue(job)) {
            LoggingManager.logError(
                component = "SmsForegroundService",
                action = "EMAIL_QUEUE_WRITE_FAILED",
                message = "E-Mail-Auftrag konnte nicht persistiert werden - kein Versand",
                details = mapOf("sender" to MmiCodeMasker.maskNumber(group.sender))
            )
            SnackbarManager.showError(getString(R.string.warning_email_queue_write_failed))
            return
        }

        serviceScope.launch { processEmailQueue() }
    }

    /**
     * Versendet alle faelligen E-Mail-Auftraege.
     *
     * Der eigene [emailDispatchMutex] und der eigene WakeLock sind Absicht: Unter
     * [withWakeLock]/[wakeLockMutex] zu laufen wuerde den SMS-Empfang serialisieren und genau die
     * Blockade wiederherstellen, die der Umbau beseitigt.
     *
     * Die zeitliche Aufloesung des Backoffs ist der Scan-Takt: Ein Neuversuch nach einer Minute
     * findet fruehestens beim naechsten Durchlauf statt, also bis zu [QUEUE_SCAN_INTERVAL_MILLIS]
     * spaeter. Ein eigener Zeitgeber je Auftrag waere die genauere, aber auch fehleranfaelligere
     * Loesung; der haeufigste Fall - Netz war weg, Netz ist wieder da - wird ohnehin sofort ueber
     * [registerNetworkCallback] abgedeckt.
     */
    private suspend fun processEmailQueue() = withContext(Dispatchers.IO) {
        emailDispatchMutex.withLock {
            if (emailScanAfterProcessStart.compareAndSet(true, false)) {
                emailQueue.updateAll { EmailDeliveryReducer.onProcessRestart(it, System.currentTimeMillis()) }
            }
            // Holt Auftraege zurueck, deren Versuch im laufenden Prozess steckengeblieben ist -
            // etwa weil ein Empfaengerergebnis nicht schreibbar war oder die Coroutine abbrach.
            // Sicher an dieser Stelle: Der Mutex belegt, dass gerade kein Versand laeuft.
            emailQueue.updateAll { EmailDeliveryReducer.onStaleAttempt(it, System.currentTimeMillis()) }
            emailQueue.updateAll { EmailDeliveryReducer.onExpiryScan(it, System.currentTimeMillis()) }

            val due = emailQueue.all().filter {
                EmailDeliveryReducer.isDispatchDue(it, System.currentTimeMillis())
            }
            if (due.isEmpty()) return@withLock

            // Ohne Netz gar nicht erst versuchen: Ein Versuch im Funkloch wuerde nur den
            // Versuchszaehler und damit das Wiederholungsbudget aufbrauchen.
            if (!isNetworkUsable()) {
                LoggingManager.logInfo(
                    component = "SmsForegroundService",
                    action = "EMAIL_DISPATCH_DEFERRED",
                    message = "Kein Netz - E-Mail-Versand aufgeschoben",
                    details = mapOf("due_jobs" to due.size)
                )
                return@withLock
            }

            // Jeder Auftrag bekommt seinen eigenen, zu seiner Empfaengerzahl passenden WakeLock.
            // Ein gemeinsamer WakeLock ueber alle faelligen Auftraege lief bei einem Rueckstau -
            // etwa nach einem laengeren Serverausfall - ab, waehrend noch gesendet wurde.
            //
            // Die Grenze wirkt als **Startgrenze**: Ein bereits begonnener Auftrag laeuft unter
            // seinem eigenen WakeLock zu Ende, statt mittendrin gekappt zu werden. Die dadurch
            // laengstmoegliche Dauer eines Durchlaufs steht in
            // EmailDispatchBudget.worstCaseRunMillis().
            val lastStart = System.currentTimeMillis() + EmailDispatchBudget.MAX_RUN_START_MILLIS
            for ((index, job) in due.withIndex()) {
                if (System.currentTimeMillis() >= lastStart) {
                    LoggingManager.logInfo(
                        component = "SmsForegroundService",
                        action = "EMAIL_RUN_BUDGET_EXHAUSTED",
                        message = "Zeitbudget des Durchlaufs erschoepft - restliche Auftraege folgen im naechsten",
                        details = mapOf("remaining" to due.size - index)
                    )
                    break
                }
                dispatchEmailJob(job)
            }
        }
    }

    /**
     * Ein Versuch fuer einen Auftrag.
     *
     * Reihenfolge wie im SMS-Kanal: Erst die **Inbesitznahme** haltbar schreiben, dann senden.
     * Schlaegt das Schreiben fehl, unterbleibt der Versand; der Auftrag bleibt nachweislich
     * ungesendet. War ein anderer Durchlauf schneller, sendet nur dieser.
     */
    private suspend fun dispatchEmailJob(job: EmailForwardingJob) {
        val claim = emailQueue.update(job.id) {
            EmailDeliveryReducer.onAttemptStart(it, System.currentTimeMillis())
        }
        val claimed = when (claim) {
            is EmailQueueUpdate.Applied -> claim.job

            is EmailQueueUpdate.Rejected -> return

            EmailQueueUpdate.NotStored -> {
                LoggingManager.logError(
                    component = "SmsForegroundService",
                    action = "EMAIL_ATTEMPT_COMMIT_FAILED",
                    message = "Zustand ATTEMPTING nicht haltbar geschrieben - Versand unterbleibt",
                    details = mapOf("job_id" to job.id)
                )
                return
            }
        }

        // Moeglich nach einem Prozessverlust: Der letzte Empfaenger wurde noch als zugestellt
        // geschrieben, der Abschluss des Versuchs nicht mehr. Dann ist nichts mehr zu senden.
        if (claimed.pendingRecipients.isEmpty()) {
            finishEmailAttempt(job.id)
            return
        }

        val config = resolveSmtpConfig()
        if (config == null) {
            emailQueue.update(job.id) {
                EmailDeliveryReducer.onConnectionFailed(
                    it,
                    System.currentTimeMillis(),
                    EmailFailure(EmailFailureKind.CONFIGURATION, "smtp_settings_incomplete")
                )
            }
            finishEmailAttempt(job.id)
            return
        }

        val sentAt = System.currentTimeMillis()
        val recipientCount = claimed.pendingRecipients.size
        val jobDeadline = sentAt + EmailDispatchBudget.forJob(recipientCount)
        var stateWriteFailed = false

        val outcome = withEmailWakeLock(EmailDispatchBudget.wakeLockMillisForJob(recipientCount)) {
            EmailSender(config).send(
                recipients = claimed.pendingRecipients,
                subject = EmailBodyComposer.subject(claimed.sender),
                body = composeEmailBody(claimed, sentAt),
                // Stabile Message-ID je Auftrag: Eine Wiederholung nach Prozessverlust ist damit
                // fuer Server und Client als Wiederholung erkennbar.
                messageId = "${claimed.id}@${config.fromAddress.substringAfter('@', "sms-forwarder.local")}"
            ) { address, failure ->
                // Der Rueckgabewert entscheidet, ob weitergesendet wird.
                val result = emailQueue.update(job.id) {
                    if (failure == null) {
                        EmailDeliveryReducer.onRecipientDelivered(it, System.currentTimeMillis(), address)
                    } else {
                        EmailDeliveryReducer.onRecipientFailed(it, System.currentTimeMillis(), address, failure)
                    }
                }
                when {
                    // Ergebnis nicht haltbar: Der Auftragszustand bildet den tatsaechlichen
                    // Versand nicht mehr ab, jeder weitere Empfaenger vergroesserte den Schaden.
                    result == EmailQueueUpdate.NotStored -> {
                        stateWriteFailed = true
                        false
                    }

                    // Budget erschoepft: sauber aufhoeren, solange der WakeLock noch haelt. Die
                    // offenen Empfaenger bleiben vermerkt und sind im naechsten Durchlauf dran.
                    System.currentTimeMillis() >= jobDeadline -> false

                    else -> true
                }
            }
        }

        when (outcome) {
            is EmailSendOutcome.ConnectionFailed -> {
                emailQueue.update(job.id) {
                    EmailDeliveryReducer.onConnectionFailed(it, System.currentTimeMillis(), outcome.failure)
                }
                finishEmailAttempt(job.id)
            }

            EmailSendOutcome.Completed -> finishEmailAttempt(job.id)

            EmailSendOutcome.Aborted -> if (stateWriteFailed) {
                // Der Auftrag wird **nicht** abgeschlossen: Ein zugestellter Empfaenger, dessen
                // Ergebnis fehlt, gaelte sonst als offen und bekaeme die Nachricht ein zweites
                // Mal. Er bleibt in ATTEMPTING und wird ueber onStaleAttempt bzw.
                // onProcessRestart kontrolliert wieder aufgenommen.
                reportEmailStateWriteFailure(job)
            } else {
                // Beim Budgetabbruch ist der Zustand vollstaendig: Der Abschluss stellt die noch
                // offenen Empfaenger als Neuversuch ein - kein Verlust, keine Dublette.
                LoggingManager.logInfo(
                    component = "SmsForegroundService",
                    action = "EMAIL_JOB_BUDGET_EXHAUSTED",
                    message = "Zeitbudget des Auftrags erschoepft - offene Empfaenger folgen im naechsten Durchlauf",
                    details = mapOf(
                        "job_id" to job.id,
                        "recipients" to recipientCount,
                        "budget_millis" to EmailDispatchBudget.forJob(recipientCount)
                    )
                )
                finishEmailAttempt(job.id)
            }
        }
    }

    /**
     * Ein Empfaengerergebnis liess sich nicht haltbar schreiben.
     *
     * Der Vermerk liegt **ausserhalb** der Queue: Deren Schreiben ist ja gerade das, was
     * fehlgeschlagen ist. Ohne ihn bliebe der einzige Beleg ein Protokolleintrag, den niemand
     * sieht - und die moegliche Doppelzustellung waere unerklaerlich.
     */
    private fun reportEmailStateWriteFailure(job: EmailForwardingJob) {
        val recorded = runCatching {
            prefsManager.recordEmailStateWriteFailure(System.currentTimeMillis())
        }.getOrDefault(false)

        LoggingManager.logError(
            component = "SmsForegroundService",
            action = "EMAIL_STATE_WRITE_FAILED",
            message = if (recorded) {
                "Empfaengerergebnis nicht haltbar geschrieben - Versand abgebrochen, Hinweis dauerhaft vermerkt"
            } else {
                "Empfaengerergebnis nicht haltbar geschrieben - Versand abgebrochen, Hinweis NUR im Protokoll"
            },
            details = mapOf(
                "job_id" to job.id,
                "sender" to MmiCodeMasker.maskNumber(job.sender),
                "warning_persisted" to recorded
            )
        )
        SnackbarManager.showError(getString(R.string.warning_email_state_write_failed))
    }

    /** Wertet die Empfaengerergebnisse aus und meldet nur den **endgueltigen** Ausgang. */
    private fun finishEmailAttempt(jobId: String) {
        val result = emailQueue.update(jobId) {
            EmailDeliveryReducer.onAttemptFinished(it, System.currentTimeMillis())
        }

        if (result == EmailQueueUpdate.NotStored) {
            // Anders als beim Abbruch mitten im Versand droht hier keine Dublette: Die
            // Empfaengerergebnisse stehen bereits, der Auftrag bleibt in ATTEMPTING und wird
            // ueber onStaleAttempt mit leerer Empfaengerliste sauber abgeschlossen.
            LoggingManager.logError(
                component = "SmsForegroundService",
                action = "EMAIL_FINISH_COMMIT_FAILED",
                message = "Abschluss des Versuchs nicht haltbar geschrieben - Auftrag bleibt zum Wiederanlauf offen",
                details = mapOf("job_id" to jobId)
            )
            return
        }

        val job = (result as? EmailQueueUpdate.Applied)?.job ?: return

        when (job.state) {
            EmailDeliveryState.SENT -> {
                LoggingManager.logInfo(
                    component = "SmsForegroundService",
                    action = "EMAIL_FORWARD",
                    message = "SMS per E-Mail weitergeleitet",
                    details = mapOf(
                        "sender" to MmiCodeMasker.maskNumber(job.sender),
                        "recipients" to job.recipients.size,
                        "attempt" to job.attempt
                    )
                )
                SnackbarManager.showSuccess(getString(R.string.snackbar_email_forward_success))
                updateServiceStatus()
            }

            // Eine Meldung pro Fehlversuch waere Laerm: Der Nutzer sieht sie im Hintergrund
            // ohnehin nicht, und der naechste Versuch steht bevor. Gemeldet wird das Ergebnis.
            EmailDeliveryState.FAILED, EmailDeliveryState.PARTIAL -> {
                LoggingManager.logError(
                    component = "SmsForegroundService",
                    action = "EMAIL_FORWARD_FAILED",
                    message = "E-Mail-Weiterleitung endgueltig gescheitert",
                    details = mapOf(
                        "sender" to MmiCodeMasker.maskNumber(job.sender),
                        "state" to job.state.name,
                        "delivered" to job.deliveredCount,
                        "recipients" to job.recipients.size,
                        "attempt" to job.attempt,
                        "failure_kind" to (job.lastFailure?.kind?.name ?: "unknown"),
                        "return_code" to job.lastFailure?.returnCode
                    )
                )
                SnackbarManager.showError(
                    getString(R.string.snackbar_email_forward_failed_final, job.sender)
                )
            }

            EmailDeliveryState.RETRY -> LoggingManager.logWarning(
                component = "SmsForegroundService",
                action = "EMAIL_FORWARD_RETRY",
                message = "E-Mail-Versand fehlgeschlagen, Neuversuch geplant",
                details = mapOf(
                    "sender" to MmiCodeMasker.maskNumber(job.sender),
                    "attempt" to job.attempt,
                    "next_attempt_at" to job.nextAttemptAtMillis,
                    "failure_kind" to (job.lastFailure?.kind?.name ?: "unknown")
                )
            )

            else -> Unit
        }
    }

    private fun composeEmailBody(job: EmailForwardingJob, nowMillis: Long): String =
        EmailBodyComposer.body(
            sender = job.sender,
            receivedAt = formatTimestamp(job.receivedAtMillis),
            forwardedAt = formatTimestamp(nowMillis)
                .takeIf { EmailBodyComposer.needsForwardedAt(job.receivedAtMillis, nowMillis) },
            message = job.body
        )

    /** `null`, wenn die Konfiguration unvollstaendig ist - dann ist kein Versuch sinnvoll. */
    private fun resolveSmtpConfig(): SmtpConfig? {
        val host = prefsManager.getSmtpHost()
        val username = prefsManager.getSmtpUsername()
        val password = prefsManager.getSmtpPassword()
        val from = prefsManager.getEffectiveSmtpFromAddress()

        if (host.isBlank() || username.isBlank() || password.isBlank() || from.isBlank()) {
            LoggingManager.logWarning(
                component = "SmsForegroundService",
                action = "EMAIL_CONFIG_INCOMPLETE",
                message = "Unvollstaendige SMTP-Konfiguration",
                details = mapOf(
                    "has_host" to host.isNotBlank(),
                    "has_username" to username.isNotBlank(),
                    "has_credentials" to password.isNotBlank(),
                    "has_from" to from.isNotBlank()
                )
            )
            return null
        }

        return SmtpConfig(
            host = host,
            port = prefsManager.getSmtpPort(),
            security = prefsManager.getSmtpSecurity(),
            username = username,
            password = password,
            fromAddress = from
        )
    }

    /**
     * Netz vorhanden?
     *
     * `NET_CAPABILITY_VALIDATED` wird bewusst **nicht** verlangt: Ein interner SMTP-Server kann
     * ueber VPN oder LAN erreichbar sein, ohne dass Android eine oeffentliche Validierung meldet.
     * Ist der Dienst gar nicht verfuegbar, wird im Zweifel versucht statt aufgeschoben.
     */
    private fun isNetworkUsable(): Boolean = runCatching {
        val manager = getSystemService(ConnectivityManager::class.java) ?: return@runCatching true
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork)
            ?: return@runCatching false
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        // Fehlt die Berechtigung wider Erwarten, wird versucht statt aufgeschoben: Ein
        // unterbliebener Versand waere der groessere Schaden als ein vergeblicher.
    }.getOrDefault(true)

    /**
     * Eigener, auftragslokaler WakeLock fuer den SMTP-Versand.
     *
     * Getrennt von [wakeLock]: Jener wird von [withWakeLock] verwaltet, ist nicht
     * referenzgezaehlt und wuerde beim Freigeben den jeweils anderen Halter mit abraeumen.
     *
     * @param timeoutMillis aus der Empfaengerzahl abgeleitet (siehe [EmailDispatchBudget]), damit
     *   der Versand nicht laenger dauern kann als sein WakeLock haelt
     */
    private suspend fun <T> withEmailWakeLock(timeoutMillis: Long, block: suspend () -> T): T {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val lock = runCatching {
            powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, emailWakeLockTag)?.apply {
                setReferenceCounted(false)
                acquire(timeoutMillis)
            }
        }.getOrNull()

        try {
            return block()
        } finally {
            runCatching { if (lock?.isHeld == true) lock.release() }
        }
    }

    // --- Anzeige ------------------------------------------------------------------------------

    private fun updateServiceStatus() {
        val status = buildServiceStatus(prefsManager)
        updateNotification(status)
    }

    private fun formatTimestamp(millis: Long): String {
        return SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
            .format(Date(millis))
    }

    /** Kurzform fuer die Kopfzeile der Weiterleitung - jedes Zeichen kostet SMS-Laenge. */
    private fun compactTimestamp(): String {
        return SimpleDateFormat("dd.MM. HH:mm", Locale.getDefault()).format(Date())
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
            .setContentTitle(getString(R.string.notification_title_service))
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
        if (!::defaultNotificationText.isInitialized) {
            defaultNotificationText = getString(R.string.notification_running_long)
        }
        isRunning = true
        val initialStatus = buildServiceStatus(prefsManager)
        currentNotificationText = initialStatus
        startForeground(NOTIFICATION_ID, createNotification(initialStatus))
    }

    private fun buildServiceStatus(prefs: SharedPreferencesManager): String {
        return buildString {
            if (!prefs.isForwardingActive() && !prefs.isForwardSmsToEmail()) {
                append(defaultNotificationText)  // Statt "Keine Weiterleitung aktiv"
            } else {
                if (prefs.isForwardingActive()) {
                    append(getString(R.string.notification_status_sms_active))
                    prefs.getSelectedPhoneNumber().let { number ->
                        if (number.isNotEmpty()) {
                            append(" " + getString(R.string.notification_status_to_number, number))
                        }
                    }
                    append("\n")
                    append(
                        when (prefs.getForwardingVerification()) {
                            ForwardingVerification.ASSUMED_SUCCESS -> getString(R.string.forwarding_verification_assumed)
                            ForwardingVerification.CONFIRMED_SUCCESS -> getString(R.string.forwarding_verification_confirmed)
                            ForwardingVerification.UNKNOWN_NO_RESPONSE -> getString(R.string.forwarding_verification_unknown)
                            ForwardingVerification.DIAL_FAILED -> getString(R.string.forwarding_verification_failed)
                            ForwardingVerification.USER_REPORTED_FAILURE -> getString(R.string.forwarding_verification_user_failed)
                            ForwardingVerification.NOT_CHECKED -> ""
                        }
                    )
                }
                if (prefs.isForwardSmsToEmail()) {
                    if (prefs.isForwardingActive()) append("\n")
                    append(getString(R.string.notification_status_email_active))
                    val emailCount = prefs.getEmailAddresses().size
                    append(" " + getString(R.string.notification_status_email_count, emailCount))
                }
            }
        }
    }

    override fun onDestroy() {
        try {
            networkCallback?.let { callback ->
                runCatching {
                    getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(callback)
                }
            }
            networkCallback = null

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

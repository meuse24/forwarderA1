package info.meuse24.smsforwarderneoA1.data.repository

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.util.Log
import androidx.annotation.VisibleForTesting
import info.meuse24.smsforwarderneoA1.LoggingManager
import info.meuse24.smsforwarderneoA1.data.local.Logger.LogLevel
import info.meuse24.smsforwarderneoA1.data.local.Logger.LogMetadata
import info.meuse24.smsforwarderneoA1.data.local.SharedPreferencesManager
import info.meuse24.smsforwarderneoA1.domain.model.Contact
import info.meuse24.smsforwarderneoA1.PhoneNumberFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Repository für Kontaktverwaltung.
 *
 * Verantwortlichkeiten:
 * - Kontakte aus Android ContactsProvider laden
 * - ContentObserver für Kontaktänderungen
 * - Such-Index für schnelles Filtern
 * - Telefonnummer-Formatierung und Normalisierung
 */
class ContactsRepositoryImpl(
    private val prefsManager: SharedPreferencesManager
) {

    private var contentObserver: ContentObserver? = null
    private var contentResolver: ContentResolver? = null
    private var updateJob: Job? = null
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    // Lifecycle-aware Handler für ContentObserver
    private var observerHandler: Handler? = null

    // Flag um zu verhindern dass Observer nach cleanup noch aktiv ist
    private val isActive = AtomicBoolean(true)

    // Koroutinen-Scope für dieses Objekt statt statisch
    private val storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Mutex für thread-safe Zugriff auf die Listen
    private val contactsMutex = Mutex()

    // MutableStateFlow für die Kontaktliste
    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val allContacts = mutableListOf<Contact>()
    private val searchIndex = HashMap<String, MutableSet<Contact>>()
    private var currentCountryCode: String = "+43"

    private var isUpdating = AtomicBoolean(false)

    fun initialize(contentResolver: ContentResolver, countryCode: String) {
        this.currentCountryCode = countryCode
        this.contentResolver = contentResolver
        scope.launch { setupContentObserver(contentResolver) }

        // Initial load
        storeScope.launch {
            loadContacts(contentResolver)
        }
    }

    @VisibleForTesting
    suspend fun setTestContacts(contacts: List<Contact>) {
        contactsMutex.withLock {
            _contacts.value = contacts
            allContacts.clear()
            allContacts.addAll(contacts)
            rebuildSearchIndex()
        }
    }


    private fun setupContentObserver(contentResolver: ContentResolver) {
        contentObserver?.let {
            contentResolver.unregisterContentObserver(it)
            LoggingManager.log(
                LogLevel.INFO,
                LogMetadata(
                    component = "ContactsRepositoryImpl",
                    action = "UNREGISTER_OBSERVER",
                    details = emptyMap()
                ),
                "Alter ContentObserver wurde entfernt"
            )
        }

        // Erstelle lifecycle-aware Handler
        if (observerHandler == null) {
            observerHandler = Handler(Looper.getMainLooper())
        }

        contentObserver = object : ContentObserver(observerHandler) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)

                // Prüfe ob Store noch aktiv ist (Memory-Leak-Prävention)
                if (!isActive.get()) {
                    return
                }

                if (!isUpdating.compareAndSet(false, true)) {
                    scope.launch {
                        LoggingManager.log(
                            LogLevel.INFO,
                            LogMetadata(
                                component = "ContactsRepositoryImpl",
                                action = "CONTACTS_CHANGED_SKIPPED",
                                details = mapOf("reason" to "update_in_progress")
                            ),
                            "Update übersprungen - bereits in Bearbeitung"
                        )
                    }
                    return
                }

                updateJob?.cancel()
                updateJob = storeScope.launch {
                    try {
                        delay(500) // Debouncing
                        val startTime = System.currentTimeMillis()

                        contentResolver.let { resolver ->
                            withContext(Dispatchers.IO) {
                                loadContacts(resolver)
                            }

                            val duration = System.currentTimeMillis() - startTime
                            LoggingManager.log(
                                LogLevel.INFO,
                                LogMetadata(
                                    component = "ContactsRepositoryImpl",
                                    action = "CONTACTS_RELOAD",
                                    details = mapOf(
                                        "duration_ms" to duration,
                                        "contacts_count" to allContacts.size
                                    )
                                ),
                                "Kontakte erfolgreich neu geladen"
                            )
                        }
                    } catch (e: Exception) {
                        LoggingManager.log(
                            LogLevel.ERROR,
                            LogMetadata(
                                component = "ContactsRepositoryImpl",
                                action = "CONTACTS_RELOAD_ERROR",
                                details = mapOf(
                                    "error" to e.message,
                                    "error_type" to e.javaClass.simpleName
                                )
                            ),
                            "Fehler beim Neuladen der Kontakte",
                            e
                        )
                    } finally {
                        isUpdating.set(false)
                    }
                }
            }
        }

        try {
            contentObserver?.let { observer ->
                contentResolver.registerContentObserver(
                    ContactsContract.Contacts.CONTENT_URI,
                    true,
                    observer
                )
                contentResolver.registerContentObserver(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    true,
                    observer
                )
                contentResolver.registerContentObserver(
                    ContactsContract.Groups.CONTENT_URI,
                    true,
                    observer
                )

                LoggingManager.log(
                    LogLevel.INFO,
                    LogMetadata(
                        component = "ContactsRepositoryImpl",
                        action = "REGISTER_OBSERVER",
                        details = mapOf("status" to "success")
                    ),
                    "ContentObserver erfolgreich registriert"
                )
            }
        } catch (e: Exception) {
            LoggingManager.log(
                LogLevel.ERROR,
                LogMetadata(
                    component = "ContactsRepositoryImpl",
                    action = "OBSERVER_REGISTRATION_ERROR",
                    details = mapOf(
                        "error" to e.message,
                        "error_type" to e.javaClass.simpleName
                    )
                ),
                "Fehler bei der Observer-Registrierung",
                e
            )
        }
    }

    suspend fun loadContacts(contentResolver: ContentResolver) {
        contactsMutex.withLock {
            val startTime = System.currentTimeMillis()
            try {
                LoggingManager.logInfo(
                    component = "ContactsRepositoryImpl",
                    action = "LOAD_CONTACTS_START",
                    message = "Starte Laden der Kontakte"
                )

                val contacts = readContactsFromProvider(contentResolver)
                allContacts.clear()
                allContacts.addAll(contacts)
                rebuildSearchIndex()
                _contacts.value = allContacts.toList()

                LoggingManager.logInfo(
                    component = "ContactsRepositoryImpl",
                    action = "LOAD_CONTACTS",
                    details = mapOf(
                        "duration_ms" to (System.currentTimeMillis() - startTime),
                        "contacts_count" to contacts.size,
                        "index_size" to searchIndex.size
                    ),
                    message = "Kontakte erfolgreich geladen"
                )
            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "ContactsRepositoryImpl",
                    action = "LOAD_CONTACTS_ERROR",
                    details = mapOf(
                        "error" to e.message,
                        "duration_ms" to (System.currentTimeMillis() - startTime)
                    ),
                    message = "Fehler beim Laden der Kontakte",
                    error = e
                )
                throw e
            }
        }
    }

    suspend fun filterContacts(query: String): List<Contact> {
        return contactsMutex.withLock {
            val startTime = System.currentTimeMillis()
            val results = if (query.isBlank()) {
                allContacts.toList()
            } else {
                val searchTerms = query.lowercase().split(" ")
                var filteredResults = mutableSetOf<Contact>()

                // Für den ersten Suchbegriff
                searchTerms.firstOrNull()?.let { firstTerm ->
                    filteredResults = searchIndex.entries
                        .filter { (key, _) -> key.contains(firstTerm) }
                        .flatMap { it.value }
                        .toMutableSet()
                }

                // Für weitere Suchbegriffe (AND-Verknüpfung)
                searchTerms.drop(1).forEach { term ->
                    val termResults = searchIndex.entries
                        .filter { (key, _) -> key.contains(term) }
                        .flatMap { it.value }
                        .toSet()
                    filteredResults.retainAll(termResults)
                }

                filteredResults.sortedBy { it.name }
            }

            val duration = System.currentTimeMillis() - startTime
            LoggingManager.log(
                LogLevel.DEBUG,
                LogMetadata(
                    component = "ContactsRepositoryImpl",
                    action = "FILTER_CONTACTS",
                    details = mapOf(
                        "query" to query,
                        "duration_ms" to duration,
                        "results_count" to results.size,
                        "total_contacts" to allContacts.size
                    )
                ),
                "Kontakte gefiltert (${duration}ms)"
            )

            results
        }
    }


    fun cleanup() {
        val startTime = System.currentTimeMillis()
        try {
            // 1. Zuerst isActive auf false setzen um weitere Observer-Callbacks zu verhindern
            isActive.set(false)

            // 2. WICHTIG: Handler-Queue SOFORT leeren (BEVOR unregister!)
            // Verhindert dass pending Messages nach unregister noch laufen
            observerHandler?.removeCallbacksAndMessages(null)

            // 3. Jetzt sicher: Update Job canceln
            updateJob?.cancel()

            // 4. ContentObserver unregistern (Handler-Queue bereits leer)
            contentObserver?.let { observer ->
                contentResolver?.unregisterContentObserver(observer)
            }

            // 5. Cancelle Scopes (stoppt alle laufenden Coroutines)
            storeScope.cancel()
            scope.cancel()

            // 6. Jetzt können wir sicher synchron clearen (keine concurrent access mehr)
            // Kein runBlocking nötig, da alle Coroutines bereits gestoppt sind
            allContacts.clear()
            searchIndex.clear()
            _contacts.value = emptyList()

            // 7. Nullen setzen
            observerHandler = null
            contentObserver = null
            contentResolver = null

            LoggingManager.log(
                LogLevel.INFO,
                LogMetadata(
                    component = "ContactsRepositoryImpl",
                    action = "CLEANUP",
                    details = mapOf(
                        "duration_ms" to (System.currentTimeMillis() - startTime)
                    )
                ),
                "ContactsRepositoryImpl erfolgreich bereinigt"
            )
        } catch (e: Exception) {
            LoggingManager.log(
                LogLevel.ERROR,
                LogMetadata(
                    component = "ContactsRepositoryImpl",
                    action = "CLEANUP_ERROR",
                    details = mapOf(
                        "error" to e.message,
                        "duration_ms" to (System.currentTimeMillis() - startTime)
                    )
                ),
                "Fehler bei der Bereinigung des ContactsRepositoryImpl",
                e
            )
        }
    }


    fun updateCountryCode(newCode: String) {
        if (currentCountryCode != newCode) {
            LoggingManager.log(
                LogLevel.INFO,
                LogMetadata(
                    component = "ContactsRepositoryImpl",
                    action = "UPDATE_COUNTRY_CODE",
                    details = mapOf(
                        "old_code" to currentCountryCode,
                        "new_code" to newCode
                    )
                ),
                "Ländervorwahl wird aktualisiert"
            )

            currentCountryCode = newCode

            storeScope.launch {
                try {
                    contentResolver?.let {
                        loadContacts(it)
                        LoggingManager.log(
                            LogLevel.INFO,
                            LogMetadata(
                                component = "ContactsRepositoryImpl",
                                action = "COUNTRY_CODE_UPDATE_COMPLETE",
                                details = mapOf(
                                    "contacts_count" to allContacts.size
                                )
                            ),
                            "Kontakte mit neuer Ländervorwahl geladen"
                        )
                    }
                } catch (e: Exception) {
                    LoggingManager.log(
                        LogLevel.ERROR,
                        LogMetadata(
                            component = "ContactsRepositoryImpl",
                            action = "COUNTRY_CODE_UPDATE_ERROR",
                            details = mapOf(
                                "error" to e.message,
                                "error_type" to e.javaClass.simpleName
                            )
                        ),
                        "Fehler beim Aktualisieren der Kontakte mit neuer Ländervorwahl",
                        e
                    )
                }
            }
        }
    }

    private fun rebuildSearchIndex() {
        searchIndex.clear()
        allContacts.forEach { contact ->
            // Indexiere nach Namen
            contact.name.lowercase().split(" ").forEach { term ->
                searchIndex.getOrPut(term) { mutableSetOf() }.add(contact)
            }
            // Indexiere nach Telefonnummer
            contact.phoneNumber.filter { it.isDigit() }.windowed(3, 1).forEach { numberPart ->
                searchIndex.getOrPut(numberPart) { mutableSetOf() }.add(contact)
            }
        }
    }

    private fun readContactsFromProvider(contentResolver: ContentResolver): List<Contact> {
        // HashMap zur Gruppierung von Kontakten mit gleicher Nummer
        val contactGroups = mutableMapOf<String, MutableList<Contact>>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.IS_PRIMARY
        )

        val phoneFormatter = PhoneNumberFormatter()

        val defaultRegion = when (currentCountryCode) {
            "+49" -> "DE"
            "+41" -> "CH"
            else -> "AT"
        }

        try {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val nameIndex =
                    cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex =
                    cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val typeIndex =
                    cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE)

                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex).orEmpty().trim()
                    val phoneNumber = cursor.getString(numberIndex).orEmpty()
                    val type = cursor.getInt(typeIndex)

                    // Prüfe ob Telefonnummern-Formatierung aktiviert ist
                    val isFormattingEnabled = prefsManager.isPhoneNumberFormattingEnabled()

                    val finalPhoneNumber: String
                    val description: String
                    val normalizedNumber: String

                    if (isFormattingEnabled) {
                        // Formatiert die Telefonnummer
                        val numberResult = phoneFormatter.formatPhoneNumber(phoneNumber, defaultRegion)

                        if (name.isNotBlank() && numberResult.formattedNumber != null) {
                            finalPhoneNumber = numberResult.formattedNumber.replace(" ", "")
                            // Normalisierte Nummer für Map-Key
                            normalizedNumber = finalPhoneNumber.filter { it.isDigit() }

                            // Erstellt die beschreibende Information mit Formatierung
                            description = buildString {
                                append(numberResult.formattedNumber)
                                numberResult.carrierInfo?.let { carrier ->
                                    append(" | ")
                                    append(carrier)
                                }
                                // Optional: Typ der Telefonnummer hinzufügen
                                append(" | ")
                                append(getPhoneTypeLabel(type))
                            }
                        } else {
                            continue // Überspringe ungültige Nummern
                        }
                    } else {
                        // Verwende die Originalnummer ohne Formatierung
                        if (name.isNotBlank() && phoneNumber.isNotBlank()) {
                            finalPhoneNumber = phoneNumber.trim()
                            normalizedNumber = finalPhoneNumber.filter { it.isDigit() }

                            // Einfache Beschreibung ohne Formatierung
                            description = buildString {
                                append(phoneNumber.trim())
                                append(" | ")
                                append(getPhoneTypeLabel(type))
                            }
                        } else {
                            continue // Überspringe leere Nummern
                        }
                    }

                    val contact = Contact(
                        name = name,
                        phoneNumber = finalPhoneNumber,
                        description = description
                    )

                    // Füge den Kontakt zur entsprechenden Gruppe hinzu
                    contactGroups.getOrPut(normalizedNumber) { mutableListOf() }.add(contact)
                }
            }
        } catch (e: Exception) {
            Log.e("ContactsRepositoryImpl", "Error reading contacts", e)
        }

        // Wähle aus jeder Gruppe den "besten" Kontakt aus
        val finalContacts = mutableListOf<Contact>()
        for (contacts in contactGroups.values) {
            if (contacts.size == 1) {
                // Wenn nur ein Kontakt, füge ihn direkt hinzu
                finalContacts.add(contacts.first())
            } else {
                // Bei mehreren Kontakten mit der gleichen Nummer, wähle den besten aus
                val bestContact = contacts.maxWithOrNull(::compareContacts)
                bestContact?.let { finalContacts.add(it) }
            }
        }

        return finalContacts.sortedBy { it.name }
    }

    // Hilfsfunktion zum Vergleichen von Kontakten
    private fun compareContacts(a: Contact, b: Contact): Int {
        // Längere Namen bevorzugen (oft enthalten diese mehr Informationen)
        val lengthComparison = a.name.length.compareTo(b.name.length)
        if (lengthComparison != 0) return lengthComparison

        // Bei gleicher Länge alphabetisch sortieren
        return a.name.compareTo(b.name)
    }

    private fun getPhoneTypeLabel(type: Int): String {
        return when (type) {
            ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Privat"
            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobil"
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Geschäftlich"
            else -> "Sonstige"
        }
    }

}

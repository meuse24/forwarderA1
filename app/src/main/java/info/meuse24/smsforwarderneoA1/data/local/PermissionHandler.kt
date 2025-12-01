package info.meuse24.smsforwarderneoA1.data.local

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import info.meuse24.smsforwarderneoA1.MainActivity

/**
 * Permission handler for runtime permission requests.
 *
 * Manages required permissions for SMS, contacts, and phone access.
 * Uses ActivityResultContracts for permission requests.
 */
class PermissionHandler(private val activity: MainActivity) {

    private val requiredPermissions: Array<String> = getRequiredPermissions()

    private val requestLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        permissionCallback?.invoke(allGranted)
        permissionCallback = null
    }

    private var permissionCallback: ((Boolean) -> Unit)? = null

    fun checkPermissions(
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        if (hasPermissions()) {
            onGranted()
        } else {
            permissionCallback = { granted ->
                if (granted) onGranted() else onDenied()
            }
            requestPermissions()
        }
    }

    private fun hasPermissions(): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Öffentliche Methode zum Prüfen ob alle Berechtigungen erteilt sind.
     * Kann von außen aufgerufen werden (z.B. in onResume()).
     */
    fun hasAllPermissions(): Boolean {
        return hasPermissions()
    }

    /**
     * Gibt eine Liste aller fehlenden Berechtigungen zurück.
     */
    fun getMissingPermissions(): List<String> {
        return requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Prüft Berechtigungen erneut und fordert fehlende an.
     * Wird verwendet wenn Berechtigungen während der Laufzeit widerrufen wurden.
     */
    fun recheckAndRequest(
        onAllGranted: () -> Unit,
        onStillMissing: (List<String>) -> Unit
    ) {
        val missing = getMissingPermissions()
        if (missing.isEmpty()) {
            onAllGranted()
        } else {
            permissionCallback = { granted ->
                if (granted) {
                    onAllGranted()
                } else {
                    onStillMissing(getMissingPermissions())
                }
            }
            requestPermissions()
        }
    }

    private fun requestPermissions() {
        try {
            requestLauncher.launch(requiredPermissions)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request permissions", e)
            permissionCallback?.invoke(false) // Falls etwas schiefgeht, verweigere Zugriff
        }
    }

    private fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PERMISSIONS_TIRAMISU
        } else {
            PERMISSIONS_BASE
        }
    }

    companion object {
        private const val TAG = "PermissionHandler"

        private val PERMISSIONS_BASE = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_PHONE_NUMBERS
        )

        @SuppressLint("InlinedApi")
        private val PERMISSIONS_TIRAMISU = PERMISSIONS_BASE + arrayOf(
            Manifest.permission.POST_NOTIFICATIONS
        )
    }
}

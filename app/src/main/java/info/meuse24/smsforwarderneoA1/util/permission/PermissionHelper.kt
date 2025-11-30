package info.meuse24.smsforwarderneoA1.util.permission

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Helper object for checking runtime permissions.
 *
 * Provides simple utility methods to check if permissions are granted.
 */
object PermissionHelper {

    /**
     * Überprüft, ob alle angegebenen Berechtigungen erteilt wurden.
     * @param context Der Anwendungskontext
     * @param permissions Eine Liste von Berechtigungen, die überprüft werden sollen
     * @return true, wenn alle Berechtigungen erteilt wurden, sonst false
     */
    fun hasPermissions(context: Context, permissions: List<String>): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Überprüft, ob eine einzelne Berechtigung erteilt wurde.
     * @param context Der Anwendungskontext
     * @param permission Die zu überprüfende Berechtigung
     * @return true, wenn die Berechtigung erteilt wurde, sonst false
     */
    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}

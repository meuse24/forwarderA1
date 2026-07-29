package info.meuse24.smsforwarderneoA1.util.phone

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.result.contract.ActivityResultContract

/**
 * Laesst den Nutzer **eine konkrete Rufnummer** statt eines Kontakts auswaehlen.
 *
 * Gegenueber `ActivityResultContracts.PickContact` hat das zwei Vorteile, die beide auf
 * dieselbe Ursache zurueckgehen - das Ergebnis ist bereits die Datenzeile der Rufnummer
 * und nicht nur der Kontakt:
 *
 * 1. Bei mehreren hinterlegten Nummern entscheidet der Nutzer, nicht die Reihenfolge des
 *    Providers.
 * 2. Die zurueckgegebene URI traegt eine temporaere Leseberechtigung. Die Rufnummer laesst
 *    sich daraus direkt lesen, ohne die allgemeine Nummerntabelle abzufragen - und damit
 *    ohne `READ_CONTACTS`.
 *
 * Es gibt dafuer keinen mitgelieferten Vertrag; `PickContact` liefert bewusst nur die
 * Kontakt-URI.
 */
class PickPhoneNumber : ActivityResultContract<Unit, Uri?>() {

    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        intent.takeIf { resultCode == Activity.RESULT_OK }?.data
}

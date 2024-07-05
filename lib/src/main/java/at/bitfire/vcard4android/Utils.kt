/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android

import android.accounts.Account
import android.content.ContentValues
import android.database.Cursor
import android.database.DatabaseUtils
import android.net.Uri
import android.provider.ContactsContract
import ezvcard.property.StructuredName

object Utils {

    fun Cursor.toContentValues(): ContentValues {
        val values = ContentValues(columnCount)
        DatabaseUtils.cursorRowToContentValues(this, values)
        return values
    }

    fun StructuredName.isEmpty() =
        prefixes.isEmpty() && given == null && additionalNames.isEmpty() && family == null && suffixes.isEmpty()

    fun Uri.asSyncAdapter(account: Account): Uri = buildUpon()
        .appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_NAME, account.name)
        .appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_TYPE, account.type)
        .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
        .build()

    fun Long.byteCountToDisplaySize(): String {
        val gb = this / 1_073_741_824 // 2^30
        if (gb > 0) {
            return "$gb GB"
        }
        val mb = this / 1_048_576 // 2^20
        if (mb > 0) {
            return "$mb MB"
        }
        val kb = this / 1024 // 2^10
        if (kb > 0) {
            return "$kb KB"
        }
        return "$this B"
    }

}
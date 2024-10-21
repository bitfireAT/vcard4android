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
import java.util.Locale

object Utils {

    fun Cursor.toContentValues(): ContentValues {
        val values = ContentValues(columnCount)
        DatabaseUtils.cursorRowToContentValues(this, values)
        return values
    }

    fun String.capitalize(): String = split(' ').joinToString(" ") { word ->
        word.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }

    /**
     * Returns a string having leading and trailing whitespace removed.
     * If the resulting string is empty, returns null.
     */
    fun String?.trimToNull(): String? = this?.trim()?.takeIf { it.isNotBlank() }

    fun StructuredName.isEmpty() =
        prefixes.isEmpty() && given == null && additionalNames.isEmpty() && family == null && suffixes.isEmpty()

    fun Uri.asSyncAdapter(addressBookAccount: Account): Uri = buildUpon()
        .appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_NAME, addressBookAccount.name)
        .appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_TYPE, addressBookAccount.type)
        .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
        .build()

}
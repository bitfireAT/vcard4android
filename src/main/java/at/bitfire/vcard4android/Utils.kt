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

object Utils {

    fun Cursor.toContentValues(): ContentValues {
        val values = ContentValues(columnCount)
        DatabaseUtils.cursorRowToContentValues(this, values)
        return values
    }

    fun Uri.asSyncAdapter(account: Account): Uri = buildUpon()
        .appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_NAME, account.name)
        .appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_TYPE, account.type)
        .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
        .build()

}
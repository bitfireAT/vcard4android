/*
 * Copyright © Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.vcard4android

import android.accounts.Account
import android.content.ContentProviderClient
import android.content.ContentValues
import android.database.DatabaseUtils
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.Groups
import android.provider.ContactsContract.RawContacts
import java.io.FileNotFoundException
import java.util.*

open class AndroidAddressBook<T1: AndroidContact, T2: AndroidGroup>(
        var account: Account,
        val provider: ContentProviderClient?,
        val contactFactory: AndroidContactFactory<T1>,
        val groupFactory: AndroidGroupFactory<T2>
) {

    // account-specific address book settings

    /**
     * Retrieves [ContactsContract.Settings] for the current address book.
     * @throws FileNotFoundException if the settings row couldn't be fetched.
     */
    fun getSettings(): ContentValues {
        provider!!.query(syncAdapterURI(ContactsContract.Settings.CONTENT_URI), null, null, null, null)?.use { cursor ->
            if (cursor.moveToNext()) {
                val values = ContentValues(cursor.columnCount)
                DatabaseUtils.cursorRowToContentValues(cursor, values)
                return values
            }
        }
        throw FileNotFoundException()
    }

    /**
     * Updates [ContactsContract.Settings] by inserting the given values into
     * the current address book.
     * @param values settings to be updated
     */
    fun updateSettings(values: ContentValues) {
        values.put(ContactsContract.Settings.ACCOUNT_NAME, account.name)
        values.put(ContactsContract.Settings.ACCOUNT_TYPE, account.type)
        provider!!.insert(syncAdapterURI(ContactsContract.Settings.CONTENT_URI), values)
    }


    // account-specific address book sync state

    fun readSyncState(): ByteArray? = ContactsContract.SyncState.get(provider, account)
    fun writeSyncState(data: ByteArray) = ContactsContract.SyncState.set(provider, account, data)


    // groups

    protected fun queryContacts(where: String?, whereArgs: Array<String>?): List<T1> {
        val contacts = LinkedList<T1>()
        provider!!.query(rawContactsSyncUri(),
                arrayOf(RawContacts._ID, AndroidContact.COLUMN_FILENAME, AndroidContact.COLUMN_ETAG),
                where, whereArgs, null)?.let { cursor ->
            while (cursor.moveToNext())
                contacts += contactFactory.newInstance(this, cursor.getLong(0), cursor.getString(1), cursor.getString(2))
        }
        return contacts
    }

    fun queryGroups(where: String?, whereArgs: Array<String>?): List<T2> {
        val groups = LinkedList<T2>()
        provider!!.query(groupsSyncUri(),
                arrayOf(Groups._ID, AndroidGroup.COLUMN_FILENAME, AndroidGroup.COLUMN_ETAG),
                where, whereArgs, null)?.use { cursor ->
            while (cursor.moveToNext())
                groups += groupFactory.newInstance(this, cursor.getLong(0), cursor.getString(1), cursor.getString(2))
        }
        return groups
    }


    // helpers

    fun syncAdapterURI(uri: Uri) = uri.buildUpon()
                .appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name)
                .appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type)
                .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                .build()!!

    fun rawContactsSyncUri() = syncAdapterURI(RawContacts.CONTENT_URI)
    fun groupsSyncUri() = syncAdapterURI(Groups.CONTENT_URI)

}

/*
 * Copyright © Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.vcard4android

import android.accounts.Account
import android.annotation.SuppressLint
import android.content.ContentProviderClient
import android.content.ContentValues
import android.database.DatabaseUtils
import android.net.Uri
import android.os.RemoteException
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

    @Throws(ContactsStorageException::class)
	fun getSettings(): ContentValues {
        val values = ContentValues()
        try {
			provider!!.query(syncAdapterURI(ContactsContract.Settings.CONTENT_URI), null, null, null, null)?.use { cursor ->
                if (cursor.moveToNext())
                    DatabaseUtils.cursorRowToContentValues(cursor, values)
                else
                    throw FileNotFoundException()
            }
		} catch(e: Exception) {
			throw ContactsStorageException("Couldn't read address book settings", e)
		}
        return values
	}

    @Throws(ContactsStorageException::class)
	fun updateSettings(values: ContentValues) {
		values.put(ContactsContract.Settings.ACCOUNT_NAME, account.name)
		values.put(ContactsContract.Settings.ACCOUNT_TYPE, account.type)
		try {
			provider!!.insert(syncAdapterURI(ContactsContract.Settings.CONTENT_URI), values)
		} catch(e: RemoteException) {
			throw ContactsStorageException("Couldn't write address book settings", e)
		}
	}


	// account-specific address book sync state

    @Throws(ContactsStorageException::class)
	fun getSyncState(): ByteArray =
		try {
			ContactsContract.SyncState.get(provider, account)
		} catch(e: RemoteException) {
			throw ContactsStorageException("Couldn't read address book sync state", e)
		}

    @Throws(ContactsStorageException::class)
	fun setSyncState(data: ByteArray) {
		try {
			ContactsContract.SyncState.set(provider, account, data)
		} catch(e: RemoteException) {
			throw ContactsStorageException("Couldn't write contacts sync state", e)
		}
	}


	// groups

    @SuppressLint("Recycle")
    @Throws(ContactsStorageException::class)
    protected fun queryContacts(where: String?, whereArgs: Array<String>?): List<T1> {
        val contacts = LinkedList<T1>()
        try {
            provider!!.query(syncAdapterURI(RawContacts.CONTENT_URI),
                    arrayOf(RawContacts._ID, AndroidContact.COLUMN_FILENAME, AndroidContact.COLUMN_ETAG),
                    where, whereArgs, null)?.let { cursor ->
                while (cursor.moveToNext())
                    contacts += contactFactory.newInstance(this, cursor.getLong(0), cursor.getString(1), cursor.getString(2))
            }
        } catch(e: RemoteException) {
            throw ContactsStorageException("Couldn't query contacts", e)
        }
        return contacts
    }

    @SuppressLint("Recycle")
    @Throws(ContactsStorageException::class)
	protected fun queryGroups(where: String?, whereArgs: Array<String>?): List<T2> {
        val groups = LinkedList<T2>()
		try {
			provider!!.query(syncAdapterURI(Groups.CONTENT_URI),
					arrayOf(Groups._ID, AndroidGroup.COLUMN_FILENAME, AndroidGroup.COLUMN_ETAG),
                    where, whereArgs, null)?.use { cursor ->
                while (cursor.moveToNext())
                    groups += groupFactory.newInstance(this, cursor.getLong(0), cursor.getString(1), cursor.getString(2))
            }
		} catch(e: RemoteException) {
			throw ContactsStorageException("Couldn't query contact groups", e)
		}
        return groups
	}


	// helpers

	fun syncAdapterURI(uri: Uri) = uri.buildUpon()
				.appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name)
				.appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type)
				.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
				.build()!!

}

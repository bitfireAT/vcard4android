/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android

import android.accounts.Account
import android.content.ContentProviderClient
import android.content.ContentValues
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.Groups
import android.provider.ContactsContract.RawContacts
import at.bitfire.vcard4android.Utils.toContentValues
import java.io.FileNotFoundException
import java.util.*

open class AndroidAddressBook<T1: AndroidContact, T2: AndroidGroup>(
        var account: Account,
        val provider: ContentProviderClient?,
        protected val contactFactory: AndroidContactFactory<T1>,
        protected val groupFactory: AndroidGroupFactory<T2>
) {

    open var readOnly: Boolean = false

    var settings: ContentValues
        /**
         * Retrieves [ContactsContract.Settings] for the current address book.
         * @throws FileNotFoundException if the settings row couldn't be fetched.
         * @throws android.os.RemoteException on content provider errors
         */
        get() {
            provider!!.query(syncAdapterURI(ContactsContract.Settings.CONTENT_URI), null, null, null, null)?.use { cursor ->
                if (cursor.moveToNext())
                    return cursor.toContentValues()
            }
            throw FileNotFoundException()
        }

        /**
         * Updates [ContactsContract.Settings] by inserting the given values into
         * the current address book.
         * @param values settings to be updated
         * @throws android.os.RemoteException on content provider errors
         */
        set(values) {
            values.put(ContactsContract.Settings.ACCOUNT_NAME, account.name)
            values.put(ContactsContract.Settings.ACCOUNT_TYPE, account.type)
            provider!!.insert(syncAdapterURI(ContactsContract.Settings.CONTENT_URI), values)
        }

    var syncState: ByteArray?
        get() = ContactsContract.SyncState.get(provider, account)
        set(data) = ContactsContract.SyncState.set(provider, account, data)


    fun queryContacts(where: String?, whereArgs: Array<String>?): List<T1> {
        val contacts = LinkedList<T1>()
        provider!!.query(rawContactsSyncUri(), null,
                where, whereArgs, null)?.use { cursor ->
            while (cursor.moveToNext())
                contacts += contactFactory.fromProvider(this, cursor.toContentValues())
        }
        return contacts
    }

    fun queryGroups(where: String?, whereArgs: Array<String>?, callback: (T2) -> Unit) {
        provider!!.query(groupsSyncUri(), null,
            where, whereArgs, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val group = groupFactory.fromProvider(this, cursor.toContentValues())
                callback(group)
            }
        }
    }

    fun queryGroups(where: String?, whereArgs: Array<String>?): List<T2> {
        val groups = LinkedList<T2>()
        queryGroups(where, whereArgs) { group ->
            groups += group
        }
        return groups
    }


    fun allGroups(callback: (T2) -> Unit) {
        queryGroups("${Groups.ACCOUNT_TYPE}=? AND ${Groups.ACCOUNT_NAME}=?", arrayOf(account.type, account.name)) { group ->
            callback(group)
        }
    }

    @Throws(FileNotFoundException::class)
    fun findContactById(id: Long) =
            queryContacts("${RawContacts._ID}=?", arrayOf(id.toString())).firstOrNull() ?: throw FileNotFoundException()

    fun findContactByUid(uid: String) =
            queryContacts("${AndroidContact.COLUMN_UID}=?", arrayOf(uid)).firstOrNull()

    @Throws(FileNotFoundException::class)
    fun findGroupById(id: Long) =
        queryGroups("${Groups._ID}=?", arrayOf(id.toString())).firstOrNull() ?: throw FileNotFoundException()


    // helpers

    fun syncAdapterURI(uri: Uri) = uri.buildUpon()
            .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name)
            .appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type)
            .build()!!

    fun rawContactsSyncUri() = syncAdapterURI(RawContacts.CONTENT_URI)
    fun groupsSyncUri() = syncAdapterURI(Groups.CONTENT_URI)

}

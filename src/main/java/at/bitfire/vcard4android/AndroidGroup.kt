/*
 * Copyright © Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.vcard4android

import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import android.os.RemoteException
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.GroupMembership
import android.provider.ContactsContract.Groups
import android.provider.ContactsContract.RawContacts
import android.provider.ContactsContract.RawContacts.Data
import org.apache.commons.lang3.builder.ToStringBuilder
import java.io.FileNotFoundException

//FIXME @ToString(of={"id","fileName","contact"},doNotUseGetters=true)
open class AndroidGroup(
        val addressBook: AndroidAddressBook<out AndroidContact, out AndroidGroup>
) {

    companion object {
        @JvmField val COLUMN_FILENAME = Groups.SOURCE_ID
        @JvmField val COLUMN_UID = Groups.SYNC1
        @JvmField val COLUMN_ETAG = Groups.SYNC2
    }

    var id: Long? = null

    var fileName: String? = null
    var eTag: String? = null

	constructor(addressBook: AndroidAddressBook<out AndroidContact, out AndroidGroup>, id: Long, fileName: String?, eTag: String?): this(addressBook) {
		this.id = id
        this.fileName = fileName
        this.eTag = eTag
	}

    constructor(addressBook: AndroidAddressBook<out AndroidContact, out AndroidGroup>, contact: Contact, fileName: String?  = null, eTag: String? = null): this(addressBook) {
		this.contact = contact
        this.fileName = fileName
        this.eTag = eTag
	}


    var contact: Contact? = null
    /**
     * Creates a {@link Contact} (representation of a VCard) from the group.
     * @throws IllegalArgumentException if group is not persistent yet ({@link #id} is null)
     */
    @Throws(FileNotFoundException::class, ContactsStorageException::class)
    get() {
        field?.let { return field }

        val id = requireNotNull(id)
        val c = Contact()
        try {
			addressBook.provider!!.query(addressBook.syncAdapterURI(ContentUris.withAppendedId(Groups.CONTENT_URI, id)),
					arrayOf(COLUMN_UID, Groups.TITLE, Groups.NOTES), null, null, null)?.use { cursor ->
                if (!cursor.moveToNext())
                    throw FileNotFoundException("Contact group not found")

                c.uid = cursor.getString(0)
                c.group = true
                c.displayName = cursor.getString(1)
                c.note = cursor.getString(2)
            }

            // query UIDs of all contacts which are member of the group
            addressBook.provider.query(addressBook.syncAdapterURI(ContactsContract.Data.CONTENT_URI),
                    arrayOf(Data.RAW_CONTACT_ID),
                    GroupMembership.MIMETYPE + "=? AND " + GroupMembership.GROUP_ROW_ID + "=?",
                    arrayOf(GroupMembership.CONTENT_ITEM_TYPE, id.toString()), null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val contactID = cursor.getLong(0)
                    Constants.log.fine("Member ID: $contactID")

                    addressBook.provider.query(addressBook.syncAdapterURI(ContentUris.withAppendedId(RawContacts.CONTENT_URI, contactID)),
                            arrayOf(AndroidContact.COLUMN_UID), null, null, null)?.use { cursor ->
                        if (cursor.moveToNext()) {
                            val uid = cursor.getString(0)
                            if (!uid.isNullOrEmpty()) {
                                Constants.log.fine("Found member of group: $uid")
                                c.members += uid
                            }
                        }
                    }
                }
            }

            field = c
            return c
		} catch (e: RemoteException) {
			throw ContactsStorageException("Couldn't read contact group", e)
		}
	}


    @Throws(FileNotFoundException::class, ContactsStorageException::class)
    protected open fun contentValues(): ContentValues {
        val values = ContentValues()
        values.put(COLUMN_FILENAME, fileName)
        values.put(COLUMN_ETAG, eTag)
        contact?.let {
            values.put(COLUMN_UID, it.uid)
            values.put(Groups.TITLE, it.displayName)
            values.put(Groups.NOTES, it.note)
        }
        return values
    }

    /**
     * Creates a group with data taken from the constructor.
     * @return number of affected rows
     * @throws ContactsStorageException in case of content provider exception
     */
    @Throws(ContactsStorageException::class)
    fun create(): Uri {
        val values = contentValues()
		values.put(Groups.ACCOUNT_TYPE, addressBook.account.type)
		values.put(Groups.ACCOUNT_NAME, addressBook.account.name)
        values.put(Groups.SHOULD_SYNC, 1)
        // read-only: values.put(Groups.GROUP_VISIBLE, 1);
		try {
			val uri = addressBook.provider!!.insert(addressBook.syncAdapterURI(Groups.CONTENT_URI), values)
			id = ContentUris.parseId(uri)
			return uri
		} catch (e: RemoteException) {
			throw ContactsStorageException("Couldn't create contact group", e)
		}
	}

    @Throws(ContactsStorageException::class)
    fun delete() =
		try {
			addressBook.provider!!.delete(groupSyncURI(), null, null)
		} catch (e: RemoteException) {
			throw ContactsStorageException("Couldn't delete contact group", e)
		}

    @Throws(ContactsStorageException::class)
    fun update(values: ContentValues) =
        try {
            addressBook.provider!!.update(groupSyncURI(), values, null, null)
        } catch (e: RemoteException) {
            throw ContactsStorageException("Couldn't delete contact group", e)
        }

    /**
     * Updates a group from a {@link Contact}, which represents a VCard received from the
     * CardDAV server.
     * @param contact data object to take group title, members etc. from
     * @return number of affected rows
     * @throws ContactsStorageException in case of a content provider exception
     */
    @Throws(ContactsStorageException::class)
    fun updateFromServer(contact: Contact): Int {
        this.contact = contact
        return update(contentValues())
    }


    override fun toString() = ToStringBuilder.reflectionToString(this)!!


    // helpers

    private fun groupSyncURI(): Uri {
        val id = requireNotNull(id)
        return addressBook.syncAdapterURI(ContentUris.withAppendedId(ContactsContract.Groups.CONTENT_URI, id))
    }

}

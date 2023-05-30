/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

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
import androidx.annotation.CallSuper
import org.apache.commons.lang3.builder.ToStringBuilder
import java.io.FileNotFoundException

open class AndroidGroup(
        val addressBook: AndroidAddressBook<out AndroidContact, out AndroidGroup>
) {

    companion object {
        const val COLUMN_FILENAME = Groups.SOURCE_ID
        const val COLUMN_UID = Groups.SYNC1
        const val COLUMN_ETAG = Groups.SYNC2
    }

    var id: Long? = null

    var fileName: String? = null
    var eTag: String? = null

	constructor(addressBook: AndroidAddressBook<out AndroidContact, out AndroidGroup>, values: ContentValues): this(addressBook) {
        id = values.getAsLong(Groups._ID)
        fileName = values.getAsString(COLUMN_FILENAME)
        eTag = values.getAsString(COLUMN_ETAG)
	}

    constructor(addressBook: AndroidAddressBook<out AndroidContact, out AndroidGroup>, contact: Contact, fileName: String?  = null, eTag: String? = null): this(addressBook) {
		_contact = contact
        this.fileName = fileName
        this.eTag = eTag
	}


    /**
     * Cached copy of the [Contact]. If this is null, [getContact] must generate the [Contact]
     * from the database and then set this property.
     */
    protected var _contact: Contact? = null

    /**
     * Fetches group data from the content provider.
     *
     * @throws IllegalArgumentException if group has not been saved yet
     * @throws FileNotFoundException when the group is not available (anymore)
     * @throws RemoteException on contact provider errors
     */
     fun getContact(): Contact {
        _contact?.let { return it }

        val id = requireNotNull(id)
        val contact = Contact()
        addressBook.provider!!.query(addressBook.syncAdapterURI(ContentUris.withAppendedId(Groups.CONTENT_URI, id)),
                arrayOf(COLUMN_UID, Groups.TITLE, Groups.NOTES), null, null, null)?.use { cursor ->
            if (!cursor.moveToNext())
                throw FileNotFoundException("Contact group not found")

            contact.group = true
            contact.uid = cursor.getString(0)
            contact.displayName = cursor.getString(1)
            contact.note = cursor.getString(2)
        }

        // get all contacts which are member of the group
        addressBook.provider.query(addressBook.syncAdapterURI(ContactsContract.Data.CONTENT_URI),
                arrayOf(Data.RAW_CONTACT_ID),
                GroupMembership.MIMETYPE + "=? AND " + GroupMembership.GROUP_ROW_ID + "=?",
                arrayOf(GroupMembership.CONTENT_ITEM_TYPE, id.toString()), null)?.use { membershipCursor ->
            while (membershipCursor.moveToNext()) {
                val contactId = membershipCursor.getLong(0)
                Constants.log.fine("Member ID: $contactId")

                // get UID from the member
                addressBook.provider.query(addressBook.syncAdapterURI(ContentUris.withAppendedId(RawContacts.CONTENT_URI, contactId)),
                        arrayOf(AndroidContact.COLUMN_UID), null, null, null)?.use { rawContactCursor ->
                    if (rawContactCursor.moveToNext()) {
                        val uid = rawContactCursor.getString(0)
                        if (!uid.isNullOrBlank()) {
                            Constants.log.fine("Found member of group: $uid")
                            // add UID to contact members (vCard MEMBERS field)
                            contact.members += uid
                        } else
                            Constants.log.severe("Couldn't add member $contactId to group contact because it doesn't have an UID (yet)")
                    }
                }
            }
        }

        _contact = contact
        return contact
    }


    @CallSuper
    protected open fun contentValues(): ContentValues {
        val values = ContentValues()
        values.put(COLUMN_FILENAME, fileName)
        values.put(COLUMN_ETAG, eTag)

        val contact = getContact()
        values.put(COLUMN_UID, contact.uid)
        values.put(Groups.TITLE, contact.displayName)
        values.put(Groups.NOTES, contact.note)
        return values
    }

    /**
     * Creates a group with data taken from the constructor.
     * @return number of affected rows
     * @throws RemoteException on contact provider errors
     * @throws ContactsStorageException when the group can't be added
     */
    fun add(): Uri {
        val values = contentValues()
		values.put(Groups.ACCOUNT_TYPE, addressBook.account.type)
		values.put(Groups.ACCOUNT_NAME, addressBook.account.name)
        values.put(Groups.SHOULD_SYNC, 1)
        if (addressBook.readOnly)
            values.put(Groups.GROUP_IS_READ_ONLY, 1)
        val uri = addressBook.provider!!.insert(addressBook.syncAdapterURI(Groups.CONTENT_URI), values)
                ?: throw ContactsStorageException("Empty result from content provider when adding group")
        id = ContentUris.parseId(uri)
        return uri
	}

    /**
     * Updates a group from a [Contact], which represents a vCard received from the
     * CardDAV server.
     * @param data data object to take group title, members etc. from
     * @return number of affected rows
     * @throws RemoteException on contact provider errors
     */
    fun update(data: Contact): Uri {
        _contact = data
        return update(contentValues())
    }

    fun update(values: ContentValues): Uri {
        val uri = groupSyncURI()
        addressBook.provider!!.update(uri, values, null, null)
        return uri
    }

    fun delete() = addressBook.provider!!.delete(groupSyncURI(), null, null)


    // helpers

    private fun groupSyncURI(): Uri {
        val id = requireNotNull(id)
        return addressBook.syncAdapterURI(ContentUris.withAppendedId(Groups.CONTENT_URI, id))
    }

    override fun toString() = ToStringBuilder.reflectionToString(this)!!

}

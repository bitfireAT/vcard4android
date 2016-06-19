/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.vcard4android;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.RawContacts.Data;

import org.apache.commons.lang3.StringUtils;

import java.io.FileNotFoundException;

import lombok.Cleanup;
import lombok.Getter;
import lombok.ToString;

@ToString(of={"id","fileName","contact"},doNotUseGetters=true)
public class AndroidGroup {
    public final static String
            COLUMN_FILENAME = Groups.SOURCE_ID,
            COLUMN_UID = Groups.SYNC1,
            COLUMN_ETAG = Groups.SYNC2;

	protected final AndroidAddressBook addressBook;

    @Getter
	protected Long id;

    @Getter
    protected String fileName;

    @Getter
    public String eTag;

	protected Contact contact;


	protected AndroidGroup(AndroidAddressBook addressBook, long id, String fileName, String eTag) {
		this.addressBook = addressBook;
		this.id = id;
        this.fileName = fileName;
        this.eTag = eTag;
	}

	protected AndroidGroup(AndroidAddressBook addressBook, Contact contact, String fileName, String eTag) {
		this.addressBook = addressBook;
		this.contact = contact;
        this.fileName = fileName;
        this.eTag = eTag;
	}


    /**
     * Creates a {@link Contact} (representation of a VCard) from the group.
     * @throws IllegalArgumentException if group is not persistent yet ({@link #id} is null)
     */
    @SuppressWarnings("Recycle")
    public Contact getContact() throws FileNotFoundException, ContactsStorageException {
        if (contact != null)
            return contact;

        assertID();
		try {
			@Cleanup Cursor cursor = addressBook.provider.query(addressBook.syncAdapterURI(ContentUris.withAppendedId(Groups.CONTENT_URI, id)),
					new String[] { COLUMN_UID, Groups.TITLE, Groups.NOTES }, null, null, null);
			if (cursor == null || !cursor.moveToNext())
				throw new FileNotFoundException("Contact group not found");

			contact = new Contact();
            contact.uid = cursor.getString(0);
            contact.group = true;
			contact.displayName = cursor.getString(1);
			contact.note = cursor.getString(2);

            // query UIDs of all contacts which are member of the group
            @Cleanup Cursor c = addressBook.provider.query(addressBook.syncAdapterURI(ContactsContract.Data.CONTENT_URI),
                    new String[] { Data.RAW_CONTACT_ID },
                    GroupMembership.MIMETYPE + "=? AND " + GroupMembership.GROUP_ROW_ID + "=?",
                    new String[] { GroupMembership.CONTENT_ITEM_TYPE, String.valueOf(id) }, null);
            while (c != null && c.moveToNext()) {
                long contactID = c.getLong(0);
                Constants.log.fine("Member ID: " + contactID);

                @Cleanup Cursor c2 = addressBook.provider.query(
                        addressBook.syncAdapterURI(ContentUris.withAppendedId(RawContacts.CONTENT_URI, contactID)),
                        new String[] { AndroidContact.COLUMN_UID },
                        null, null,
                        null
                );
                if (c2 != null && c2.moveToNext()) {
                    String uid = c2.getString(0);
                    if (!StringUtils.isEmpty(uid)) {
                        Constants.log.fine("Found member of group: " + uid);
                        contact.members.add(uid);
                    }
                }
            }

			return contact;
		} catch (RemoteException e) {
			throw new ContactsStorageException("Couldn't read contact group", e);
		}
	}


    @SuppressLint("Recycle")
    protected ContentValues contentValues() {
        ContentValues values = new ContentValues();
        values.put(COLUMN_FILENAME, fileName);
        values.put(COLUMN_UID, contact.uid);
        values.put(COLUMN_ETAG, eTag);
        values.put(Groups.TITLE, contact.displayName);
        values.put(Groups.NOTES, contact.note);
        return values;
    }

    /**
     * Creates a group with data taken from the constructor.
     * @return number of affected rows
     * @throws ContactsStorageException in case of content provider exception
     */
    public Uri create() throws ContactsStorageException {
        ContentValues values = contentValues();
		values.put(Groups.ACCOUNT_TYPE, addressBook.account.type);
		values.put(Groups.ACCOUNT_NAME, addressBook.account.name);
        values.put(Groups.SHOULD_SYNC, 1);
        // read-only: values.put(Groups.GROUP_VISIBLE, 1);
		try {
			Uri uri = addressBook.provider.insert(addressBook.syncAdapterURI(Groups.CONTENT_URI), values);
			id = ContentUris.parseId(uri);
			return uri;
		} catch (RemoteException e) {
			throw new ContactsStorageException("Couldn't create contact group", e);
		}
	}

	public int delete() throws ContactsStorageException {
		try {
			return addressBook.provider.delete(groupSyncURI(), null, null);
		} catch (RemoteException e) {
			throw new ContactsStorageException("Couldn't delete contact group", e);
		}
	}

    public int update(ContentValues values) throws ContactsStorageException {
        try {
            return addressBook.provider.update(groupSyncURI(), values, null, null);
        } catch (RemoteException e) {
            throw new ContactsStorageException("Couldn't delete contact group", e);
        }
    }

    /**
     * Updates a group from a {@link Contact}, which represents a VCard received from the
     * CardDAV server.
     * @param contact data object to take group title, members etc. from
     * @return number of affected rows
     * @throws ContactsStorageException in case of a content provider exception
     */
    public int updateFromServer(Contact contact) throws ContactsStorageException {
        this.contact = contact;
        return update(contentValues());
    }


    // helpers

    private void assertID() {
        if (id == null)
            throw new IllegalArgumentException("Group hasn't been saved yet");
    }

    protected Uri groupSyncURI() {
        assertID();
        return addressBook.syncAdapterURI(ContentUris.withAppendedId(ContactsContract.Groups.CONTENT_URI, id));
    }

}

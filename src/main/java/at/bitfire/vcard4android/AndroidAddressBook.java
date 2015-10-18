/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.vcard4android;

import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;

import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;

import lombok.Cleanup;
import lombok.Getter;

public class AndroidAddressBook {
	private static final String TAG = "vcard4android.AddrBook";

	final public Account account;
	final public ContentProviderClient provider;
	final AndroidGroupFactory groupFactory;
	final AndroidContactFactory contactFactory;

	public AndroidAddressBook(Account account, ContentProviderClient provider, AndroidGroupFactory groupFactory, AndroidContactFactory contactFactory) {
		this.account = account;
		this.provider = provider;
		this.groupFactory = groupFactory;
		this.contactFactory = contactFactory;
	}


	// account-specific address book settings

    @SuppressWarnings("Recycle")
	public ContentValues getSettings() throws ContactsStorageException {
		try {
			@Cleanup Cursor cursor = provider.query(syncAdapterURI(ContactsContract.Settings.CONTENT_URI), null, null, null, null);
			if (cursor != null && cursor.moveToNext()) {
				ContentValues values = new ContentValues();
				DatabaseUtils.cursorRowToContentValues(cursor, values);
				return values;
			} else
				throw new FileNotFoundException();
		} catch(FileNotFoundException|RemoteException e) {
			throw new ContactsStorageException("Couldn't read contacts settings", e);
		}
	}

	public void updateSettings(ContentValues values) throws ContactsStorageException {
		values.put(ContactsContract.Settings.ACCOUNT_NAME, account.name);
		values.put(ContactsContract.Settings.ACCOUNT_TYPE, account.type);
		try {
			provider.insert(syncAdapterURI(ContactsContract.Settings.CONTENT_URI), values);
		} catch(RemoteException e) {
			throw new ContactsStorageException("Couldn't write contacts settings", e);
		}
	}


	// account-specific address book sync state

	public byte[] getSyncState() throws ContactsStorageException {
		try {
			return ContactsContract.SyncState.get(provider, account);
		} catch (RemoteException e) {
			throw new ContactsStorageException("Couldn't read contacts sync state", e);
		}
	}

	public void setSyncState(byte[] data) throws ContactsStorageException {
		try {
			ContactsContract.SyncState.set(provider, account, data);
		} catch (RemoteException e) {
			throw new ContactsStorageException("Couldn't write contacts sync state", e);
		}
	}


	// groups

    @SuppressWarnings("Recycle")
	protected AndroidGroup[] queryGroups(String where, String[] whereArgs) throws ContactsStorageException {
		try {
			@Cleanup Cursor cursor = provider.query(syncAdapterURI(ContactsContract.Groups.CONTENT_URI),
					new String[] { ContactsContract.Groups._ID }, where, whereArgs, null);

			List<AndroidGroup> groups = new LinkedList<>();
			while (cursor != null && cursor.moveToNext())
				groups.add(groupFactory.newInstance(this, cursor.getLong(0)));
			return groups.toArray(groupFactory.newArray(groups.size()));
		} catch (RemoteException e) {
			throw new ContactsStorageException("Couldn't query contact groups", e);
		}
	}

    @SuppressWarnings("Recycle")
    protected AndroidContact[] queryContacts(String where, String[] whereArgs) throws ContactsStorageException {
		try {
			@Cleanup Cursor cursor = provider.query(syncAdapterURI(ContactsContract.RawContacts.CONTENT_URI),
					new String[] { ContactsContract.RawContacts._ID, AndroidContact.COLUMN_FILENAME, AndroidContact.COLUMN_ETAG },
                    where, whereArgs, null);

			List<AndroidContact> contacts = new LinkedList<>();
			while (cursor != null && cursor.moveToNext())
				contacts.add(contactFactory.newInstance(this, cursor.getLong(0), cursor.getString(1), cursor.getString(2)));
			return contacts.toArray(contactFactory.newArray(contacts.size()));
		} catch (RemoteException e) {
			throw new ContactsStorageException("Couldn't query contacts", e);
		}
	}


	// helpers

	public Uri syncAdapterURI(Uri uri) {
		return uri.buildUpon()
				.appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_NAME, account.name)
				.appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_TYPE, account.type)
				.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
				.build();
	}

}

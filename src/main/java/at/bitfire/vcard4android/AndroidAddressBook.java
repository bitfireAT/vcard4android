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

import lombok.Cleanup;

public class AndroidAddressBook {
	private static final String TAG = "vcard4android.AddrBook";

	final Account account;
	final ContentProviderClient provider;

	public AndroidAddressBook(Account account, ContentProviderClient provider) {
		this.account = account;
		this.provider = provider;
	}


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
			throw new ContactsStorageException("Couldn't read local contacts settings", e);
		}
	}

	public void updateSettings(ContentValues values) throws ContactsStorageException {
		values.put(ContactsContract.Settings.ACCOUNT_NAME, account.name);
		values.put(ContactsContract.Settings.ACCOUNT_TYPE, account.type);
		try {
			provider.insert(syncAdapterURI(ContactsContract.Settings.CONTENT_URI), values);
		} catch(RemoteException e) {
			throw new ContactsStorageException("Couldn't write local contacts settings", e);
		}
	}


	public byte[] getSyncState() throws ContactsStorageException {
		try {
			return ContactsContract.SyncState.get(provider, account);
		} catch (RemoteException e) {
			throw new ContactsStorageException("Couldn't read local contacts sync state", e);
		}
	}

	public void setSyncState(byte[] data) throws ContactsStorageException {
		try {
			ContactsContract.SyncState.set(provider, account, data);
		} catch (RemoteException e) {
			throw new ContactsStorageException("Couldn't write local contacts sync state", e);
		}
	}


	public Uri syncAdapterURI(Uri uri) {
		return uri.buildUpon()
				.appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_NAME, account.name)
				.appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_TYPE, account.type)
				.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
				.build();
	}

}

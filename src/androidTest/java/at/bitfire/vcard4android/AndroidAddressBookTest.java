/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.vcard4android;

import android.Manifest;
import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.provider.ContactsContract;
import android.support.annotation.RequiresPermission;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import at.bitfire.vcard4android.impl.TestAddressBook;

import static android.support.test.InstrumentationRegistry.getContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AndroidAddressBookTest {

	final Account testAccount = new Account("AndroidAddressBookTest", "at.bitfire.vcard4android");
	ContentProviderClient provider;

	@Before
    @RequiresPermission(allOf = { Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS })
	public void connect() throws Exception {
		provider = getContext().getContentResolver().acquireContentProviderClient(ContactsContract.AUTHORITY);
		assertNotNull(provider);
	}

	@After
	public void disconnect() throws Exception {
		provider.release();
	}


    @Test
	public void testSettings() throws ContactsStorageException {
		AndroidAddressBook addressBook = new TestAddressBook(testAccount, provider);

		ContentValues values = new ContentValues();
		values.put(ContactsContract.Settings.SHOULD_SYNC, false);
		values.put(ContactsContract.Settings.UNGROUPED_VISIBLE, false);
		addressBook.updateSettings(values);
		values = addressBook.getSettings();
		assertFalse(values.getAsInteger(ContactsContract.Settings.SHOULD_SYNC) != 0);
		assertFalse(values.getAsInteger(ContactsContract.Settings.UNGROUPED_VISIBLE) != 0);

		values = new ContentValues();
		values.put(ContactsContract.Settings.SHOULD_SYNC, true);
		values.put(ContactsContract.Settings.UNGROUPED_VISIBLE, true);
		addressBook.updateSettings(values);
		values = addressBook.getSettings();
		assertTrue(values.getAsInteger(ContactsContract.Settings.SHOULD_SYNC) != 0);
		assertTrue(values.getAsInteger(ContactsContract.Settings.UNGROUPED_VISIBLE) != 0);
	}

    @Test
    public void testSyncState() throws ContactsStorageException {
		AndroidAddressBook addressBook = new TestAddressBook(testAccount, provider);

		addressBook.writeSyncState(new byte[0]);
		assertEquals(0, addressBook.readSyncState().length);

		final byte[] random = { 1, 2, 3, 4, 5 };
		addressBook.writeSyncState(random);
		assertTrue(Arrays.equals(random, addressBook.readSyncState()));
	}

}

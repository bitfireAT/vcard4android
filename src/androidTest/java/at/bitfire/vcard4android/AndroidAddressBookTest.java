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
import android.content.Context;
import android.provider.ContactsContract;
import android.test.InstrumentationTestCase;

import java.util.Arrays;

public class AndroidAddressBookTest extends InstrumentationTestCase {

	final Account testAccount = new Account("AndroidAddressBookTest", "at.bitfire.vcard4android");
	ContentProviderClient provider;

	@Override
	protected void setUp() throws Exception {
		Context context = getInstrumentation().getContext();
		provider = context.getContentResolver().acquireContentProviderClient(ContactsContract.AUTHORITY);
		assertNotNull(provider);
	}

	@Override
	public void tearDown() throws Exception {
		provider.release();
	}


	public void testSettings() throws ContactsStorageException {
		AndroidAddressBook addressBook = new AndroidAddressBook(testAccount, provider, AndroidGroupFactory.INSTANCE, AndroidContactFactory.INSTANCE);

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

	public void testSyncState() throws ContactsStorageException {
		AndroidAddressBook addressBook = new AndroidAddressBook(testAccount, provider, AndroidGroupFactory.INSTANCE, AndroidContactFactory.INSTANCE);

		addressBook.setSyncState(new byte[0]);
		assertEquals(0, addressBook.getSyncState().length);

		final byte[] random = { 1, 2, 3, 4, 5 };
		addressBook.setSyncState(random);
		assertTrue(Arrays.equals(random, addressBook.getSyncState()));
	}

}

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
import android.content.Context;
import android.provider.ContactsContract;
import android.test.InstrumentationTestCase;

import java.io.FileNotFoundException;

public class AndroidGroupTest extends InstrumentationTestCase {

	final Account testAccount = new Account("AndroidContactGroupTest", "at.bitfire.vcard4android");
	ContentProviderClient provider;

	AndroidAddressBook addressBook;

	@Override
	protected void setUp() throws Exception {
		Context context = getInstrumentation().getContext();
		provider = context.getContentResolver().acquireContentProviderClient(ContactsContract.AUTHORITY);
		assertNotNull(provider);

		addressBook = new AndroidAddressBook(testAccount, provider, AndroidGroupFactory.INSTANCE, AndroidContactFactory.INSTANCE);
	}

	@Override
	public void tearDown() throws Exception {
		provider.release();
	}


	public void testCreateReadDeleteGroup() throws FileNotFoundException, ContactsStorageException {
		Contact contact = new Contact();
		contact.displayName = "at.bitfire.vcard4android-AndroidGroupTest";
		contact.note = "(test group)";

		// ensure we start without this group
		assertEquals(0, addressBook.queryGroups(ContactsContract.Groups.TITLE + "=?", new String[] { contact.displayName }).length);

		// create group
		AndroidGroup group = new AndroidGroup(addressBook, contact);
		group.create();
		AndroidGroup[] groups = addressBook.queryGroups(ContactsContract.Groups.TITLE + "=?", new String[] { contact.displayName } );
		assertEquals(1, groups.length);
		Contact contact2 = groups[0].getContact();
		assertEquals(contact.displayName, contact2.displayName);
		assertEquals(contact.note, contact2.note);

		// delete group
		group.delete();
		assertEquals(0, addressBook.queryGroups(ContactsContract.Groups.TITLE + "=?", new String[] { contact.displayName }).length);
	}

}

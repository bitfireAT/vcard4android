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

import lombok.Cleanup;

public class AndroidContactTest extends InstrumentationTestCase {

	final Account testAccount = new Account("AndroidContactTest", "at.bitfire.vcard4android");
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


	public void testAddAndReadContact() throws FileNotFoundException, ContactsStorageException {
		Contact vcard = new Contact();
		vcard.displayName = "Mya Contact";
		vcard.givenName = "Mya";
		vcard.familyName = "Contact";

		@Cleanup("delete") AndroidContact contact = new AndroidContact(addressBook, vcard, null, null);
		contact.add();

		AndroidContact contact2 = new AndroidContact(addressBook, contact.id, null, null);
		Contact vcard2 = contact2.getContact();
		assertEquals(vcard2.displayName, vcard.displayName);
		assertEquals(vcard2.givenName, vcard.givenName);
		assertEquals(vcard2.familyName, vcard.familyName);
	}

	public void testLabelToXName() {
		assertEquals("X-AUNTIES_HOME", AndroidContact.labelToXName("auntie's home"));
	}

	public void testXNameToLabel() {
		assertEquals("Aunties Home", AndroidContact.xNameToLabel("X-AUNTIES_HOME"));
	}

}

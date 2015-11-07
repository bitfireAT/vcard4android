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
import java.net.URI;
import java.net.URISyntaxException;

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
        vcard.prefix = "Magª";
		vcard.givenName = "Mya";
		vcard.familyName = "Contact";
        vcard.suffix = "BSc";
        vcard.phoneticGivenName = "Först";
        vcard.phoneticMiddleName = "Mittelerde";
        vcard.phoneticFamilyName = "Fämilie";

		@Cleanup("delete") AndroidContact contact = new AndroidContact(addressBook, vcard, null, null);
		contact.add();

		AndroidContact contact2 = new AndroidContact(addressBook, contact.id, null, null);
		Contact vcard2 = contact2.getContact();
		assertEquals(vcard2.displayName, vcard.displayName);
        assertEquals(vcard2.prefix, vcard.prefix);
		assertEquals(vcard2.givenName, vcard.givenName);
		assertEquals(vcard2.familyName, vcard.familyName);
        assertEquals(vcard2.suffix, vcard.suffix);
        assertEquals(vcard2.phoneticGivenName, vcard.phoneticGivenName);
        assertEquals(vcard2.phoneticMiddleName, vcard.phoneticMiddleName);
        assertEquals(vcard2.phoneticFamilyName, vcard.phoneticFamilyName);
	}


	public void testLabelToXName() {
		assertEquals("X-AUNTIES_HOME", AndroidContact.labelToXName("auntie's home"));
	}

    public void testToURIScheme() {
        assertEquals("testp+csfgh-ewt4345.2qiuz4", AndroidContact.toURIScheme("02 34test#ä{☺}ö p[]ß+csfgh()-e_wt4\\345.2qiuz4"));
        assertEquals("CyanogenModForum", AndroidContact.toURIScheme("CyanogenMod Forum"));
        assertEquals("CyanogenModForum", AndroidContact.toURIScheme("CyanogenMod_Forum"));
    }

	public void testXNameToLabel() {
		assertEquals("Aunties Home", AndroidContact.xNameToLabel("X-AUNTIES_HOME"));
	}

}

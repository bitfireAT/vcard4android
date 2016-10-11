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
import android.provider.ContactsContract;
import android.support.annotation.RequiresPermission;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;

import static android.support.test.InstrumentationRegistry.getContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AndroidGroupTest {

    final Account testAccount = new Account("AndroidContactGroupTest", "at.bitfire.vcard4android");
    ContentProviderClient provider;

    AndroidAddressBook addressBook;

    @Before
    @RequiresPermission(allOf = { Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS })
    public void connect() throws Exception {
        provider = getContext().getContentResolver().acquireContentProviderClient(ContactsContract.AUTHORITY);
        assertNotNull(provider);

        addressBook = new AndroidAddressBook(testAccount, provider, AndroidGroupFactory.INSTANCE, AndroidContactFactory.INSTANCE);
    }

    @After
    public void disconnect() throws Exception {
        provider.release();
    }


    @Test
    public void testCreateReadDeleteGroup() throws FileNotFoundException, ContactsStorageException {
        Contact contact = new Contact();
        contact.displayName = "at.bitfire.vcard4android-AndroidGroupTest";
        contact.note = "(test group)";

        // ensure we start without this group
        assertEquals(0, addressBook.queryGroups(ContactsContract.Groups.TITLE + "=?", new String[] { contact.displayName }).length);

        // create group
        AndroidGroup group = new AndroidGroup(addressBook, contact, null, null);
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

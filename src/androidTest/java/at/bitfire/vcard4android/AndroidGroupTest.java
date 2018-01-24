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
import android.support.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.List;

import at.bitfire.vcard4android.impl.TestAddressBook;

import static android.support.test.InstrumentationRegistry.getContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AndroidGroupTest {

    @Rule
    public GrantPermissionRule permissionRule =
            GrantPermissionRule.grant(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS);

    final Account testAccount = new Account("AndroidContactGroupTest", "at.bitfire.vcard4android");
    ContentProviderClient provider;

    AndroidAddressBook addressBook;

    @Before
    public void connect() throws Exception {
        provider = getContext().getContentResolver().acquireContentProviderClient(ContactsContract.AUTHORITY);
        assertNotNull(provider);

        addressBook = new TestAddressBook(testAccount, provider);
    }

    @After
    public void disconnect() throws Exception {
        provider.release();
    }


    @Test
    public void testCreateReadDeleteGroup() throws FileNotFoundException, ContactsStorageException {
        Contact contact = new Contact();
        contact.setDisplayName("at.bitfire.vcard4android-AndroidGroupTest");
        contact.setNote("(test group)");

        // ensure we start without this group
        assertEquals(0, addressBook.queryGroups(ContactsContract.Groups.TITLE + "=?", new String[] { contact.getDisplayName() }).size());

        // create group
        AndroidGroup group = new AndroidGroup(addressBook, contact, null, null);
        group.create();
        List<AndroidGroup> groups = addressBook.queryGroups(ContactsContract.Groups.TITLE + "=?", new String[] { contact.getDisplayName() } );
        assertEquals(1, groups.size());
        Contact contact2 = groups.get(0).getContact();
        assertEquals(contact.getDisplayName(), contact2.getDisplayName());
        assertEquals(contact.getNote(), contact2.getNote());

        // delete group
        group.delete();
        assertEquals(0, addressBook.queryGroups(ContactsContract.Groups.TITLE + "=?", new String[] { contact.getDisplayName() }).size());
    }

}

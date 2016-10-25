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

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import ezvcard.VCardVersion;
import ezvcard.property.Address;
import ezvcard.property.Email;
import lombok.Cleanup;

import static android.support.test.InstrumentationRegistry.getContext;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AndroidContactTest {

    final Account testAccount = new Account("AndroidContactTest", "at.bitfire.vcard4android");
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

        AndroidContact contact = new AndroidContact(addressBook, vcard, null, null);
        contact.create();

        @Cleanup("delete") AndroidContact contact2 = new AndroidContact(addressBook, contact.id, null, null);
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

    @Test
    public void testLargeTransactionManyRows() throws FileNotFoundException, ContactsStorageException {
        Contact vcard = new Contact();
        vcard.displayName = "Large Transaction (many rows)";
        for (int i = 0; i < 4000; i++)
            vcard.emails.add(new LabeledProperty<Email>(new Email("test" + i + "@example.com")));

        AndroidContact contact = new AndroidContact(addressBook, vcard, null, null);
        contact.create();

        @Cleanup("delete") AndroidContact contact2 = new AndroidContact(addressBook, contact.id, null, null);
        Contact vcard2 = contact2.getContact();
        assertEquals(4000, vcard2.emails.size());
    }

    @Test(expected = ContactsStorageException.class)
    public void testLargeTransactionSingleRow() throws FileNotFoundException, ContactsStorageException {
        Contact vcard = new Contact();
        vcard.displayName = "Large Transaction (one row which is too large)";

        // 1 MB eTag ... have fun
        char data[] = new char[1024*1024];
        Arrays.fill(data, 'x');
        String eTag = new String(data);

        AndroidContact contact = new AndroidContact(addressBook, vcard, null, eTag);
        contact.create();
    }

    @Test
    public void testAddressCaretEncoding() throws IOException {
        Address address = new Address();
        address.setLabel("My \"Label\"\nLine 2");
        address.setStreetAddress("Street \"Address\"");
        Contact contact = new Contact();
        contact.addresses.add(new LabeledProperty<>(address));

        /* label-param = "LABEL=" param-value
         * param-values must not contain DQUOTE and should be encoded as defined in RFC 6868
         *
         * ADR-value = ADR-component-pobox ";" ADR-component-ext ";"
         *             ADR-component-street ";" ADR-component-locality ";"
         *             ADR-component-region ";" ADR-component-code ";"
         *             ADR-component-country
         * ADR-component-pobox    = list-component
         *
         * list-component = component *("," component)
         * component = "\\" / "\," / "\;" / "\n" / WSP / NON-ASCII / %x21-2B / %x2D-3A / %x3C-5B / %x5D-7E
         *
         * So, ADR value components may contain DQUOTE (0x22) and don't have to be encoded as defined in RFC 6868 */

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        contact.write(VCardVersion.V4_0, GroupMethod.GROUP_VCARDS, os);
        Constants.log.info(os.toString());
        assertTrue(os.toString().contains("ADR;LABEL=My ^'Label^'\\nLine 2:;;Street \"Address\";;;;"));
    }


    @Test
    public void testLabelToXName() {
        assertEquals("X-AUNTIES_HOME", AndroidContact.labelToXName("auntie's home"));
    }

    @Test
    public void testToURIScheme() {
        assertEquals("testp+csfgh-ewt4345.2qiuz4", AndroidContact.toURIScheme("02 34test#ä{☺}ö p[]ß+csfgh()-e_wt4\\345.2qiuz4"));
        assertEquals("CyanogenModForum", AndroidContact.toURIScheme("CyanogenMod Forum"));
        assertEquals("CyanogenModForum", AndroidContact.toURIScheme("CyanogenMod_Forum"));
    }

    @Test
    public void testXNameToLabel() {
        assertEquals("Aunties Home", AndroidContact.xNameToLabel("X-AUNTIES_HOME"));
    }

}

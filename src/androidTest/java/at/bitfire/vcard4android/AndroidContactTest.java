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

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Date;
import java.util.Arrays;
import java.util.List;

import at.bitfire.vcard4android.impl.TestAddressBook;
import ezvcard.VCardVersion;
import ezvcard.property.Address;
import ezvcard.property.Birthday;
import ezvcard.property.Email;
import ezvcard.util.PartialDate;
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

        addressBook = new TestAddressBook(testAccount, provider);
    }

    @After
    public void disconnect() throws Exception {
        provider.release();
    }


    @Test
    public void testAddAndReadContact() throws ContactsStorageException, FileNotFoundException {
        Contact vcard = new Contact();
        vcard.setDisplayName("Mya Contact");
        vcard.setPrefix("Magª");
        vcard.setGivenName("Mya");
        vcard.setFamilyName("Contact");
        vcard.setSuffix("BSc");
        vcard.setPhoneticGivenName("Först");
        vcard.setPhoneticMiddleName("Mittelerde");
        vcard.setPhoneticFamilyName("Fämilie");
        vcard.setBirthDay(new Birthday(Date.valueOf("1980-04-16")));

        AndroidContact contact = new AndroidContact(addressBook, vcard, null, null);
        contact.create();

        @Cleanup("delete") AndroidContact contact2 = new AndroidContact(addressBook, contact.getId(), null, null);
        Contact vcard2 = contact2.getContact();
        assertEquals(vcard.getDisplayName(), vcard2.getDisplayName());
        assertEquals(vcard.getPrefix(), vcard2.getPrefix());
        assertEquals(vcard.getGivenName(), vcard2.getGivenName());
        assertEquals(vcard.getFamilyName(), vcard2.getFamilyName());
        assertEquals(vcard.getSuffix(), vcard2.getSuffix());
        assertEquals(vcard.getPhoneticGivenName(), vcard2.getPhoneticGivenName());
        assertEquals(vcard.getPhoneticMiddleName(), vcard2.getPhoneticMiddleName());
        assertEquals(vcard.getPhoneticFamilyName(), vcard2.getPhoneticFamilyName());
        assertEquals(vcard.getBirthDay(), vcard2.getBirthDay());
    }

    @Test
    public void testInvalidPREF() throws ContactsStorageException, IOException {
        Charset charset = Charsets.UTF_8;
        String vCard = "BEGIN:VCARD\r\n" +
                "VERSION:4.0\r\n" +
                "FN:Test\r\n" +
                "TEL;CELL=;PREF=:+12345\r\n" +
                "EMAIL;PREF=invalid:test@example.com\r\n" +
                "END:VCARD\r\n";
        List<Contact> contacts = Contact.fromStream(IOUtils.toInputStream(vCard, charset), charset, null);

        AndroidContact dbContact = new AndroidContact(addressBook, contacts.get(0), null, null);
        dbContact.create();

        @Cleanup("delete") AndroidContact dbContact2 = new AndroidContact(addressBook, dbContact.getId(), null, null);
        Contact contact2 = dbContact2.getContact();
        assertEquals("Test", contact2.getDisplayName());
        assertEquals("+12345", contact2.getPhoneNumbers().get(0).getProperty().getText());
        assertEquals("test@example.com", contact2.getEmails().get(0).getProperty().getValue());
    }

    @Test
    public void testLargeTransactionManyRows() throws ContactsStorageException, FileNotFoundException {
        Contact vcard = new Contact();
        vcard.setDisplayName("Large Transaction (many rows)");
        for (int i = 0; i < 4000; i++)
            vcard.getEmails().add(new LabeledProperty<Email>(new Email("test" + i + "@example.com")));

        AndroidContact contact = new AndroidContact(addressBook, vcard, null, null);
        contact.create();

        @Cleanup("delete") AndroidContact contact2 = new AndroidContact(addressBook, contact.getId(), null, null);
        Contact vcard2 = contact2.getContact();
        assertEquals(4000, vcard2.getEmails().size());
    }

    @Test(expected = ContactsStorageException.class)
    public void testLargeTransactionSingleRow() throws ContactsStorageException {
        Contact vcard = new Contact();
        vcard.setDisplayName("Large Transaction (one row which is too large)");

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
        contact.getAddresses().add(new LabeledProperty<>(address));

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
    public void testBirthdayWithoutYear() throws ContactsStorageException, FileNotFoundException {
        Contact vcard = new Contact();
        vcard.setDisplayName("Mya Contact");
        vcard.setBirthDay(new Birthday(PartialDate.parse("-04-16")));

        AndroidContact contact = new AndroidContact(addressBook, vcard, null, null);
        contact.create();

        @Cleanup("delete") AndroidContact contact2 = new AndroidContact(addressBook, contact.getId(), null, null);
        Contact vcard2 = contact2.getContact();
        assertEquals(vcard.getDisplayName(), vcard2.getDisplayName());
        assertEquals(vcard.getBirthDay(), vcard2.getBirthDay());
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

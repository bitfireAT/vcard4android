/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.vcard4android;

import android.content.res.AssetManager;
import android.test.InstrumentationTestCase;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;

import ezvcard.VCard;
import ezvcard.VCardVersion;
import ezvcard.io.text.VCardWriter;
import ezvcard.parameter.AddressType;
import ezvcard.parameter.EmailType;
import ezvcard.parameter.ImppType;
import ezvcard.parameter.RelatedType;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.Address;
import ezvcard.property.Email;
import ezvcard.property.Impp;
import ezvcard.property.Related;
import ezvcard.property.Telephone;
import ezvcard.property.Url;
import ezvcard.util.IOUtils;
import lombok.Cleanup;

public class ContactTest extends InstrumentationTestCase {
	private static final String TAG = "vcard4android.ContactTest";

	AssetManager assetMgr;


	public void setUp() throws IOException {
		assetMgr = getInstrumentation().getContext().getResources().getAssets();
	}

	public void testVCard3FieldsAsVCard3() throws IOException {
		Contact c = regenerate(parseContact("allfields-vcard3.vcf", null), VCardVersion.V3_0);

        // UID
        assertEquals("mostfields1@at.bitfire.vcard4android", c.uid);

        // FN
        assertEquals("Ämi Display", c.displayName);

        // N
        assertEquals("Firstname", c.givenName);
        assertEquals("Middlename", c.middleName);
        assertEquals("Lastname", c.familyName);

        // phonetic names
        assertEquals("Förstnehm", c.phoneticGivenName);
        assertEquals("Mittelnehm", c.phoneticMiddleName);
        assertEquals("Laastnehm", c.phoneticFamilyName);

        // TEL
        assertEquals(2, c.getPhoneNumbers().size());
        Telephone phone = c.getPhoneNumbers().get(0);
        assertTrue(phone.getTypes().contains(TelephoneType.VOICE));
        assertTrue(phone.getTypes().contains(TelephoneType.HOME));
        assertTrue(phone.getTypes().contains(TelephoneType.PREF));
        assertNull(phone.getPref());
        assertEquals("+49 1234 56788", phone.getText());
        phone = c.getPhoneNumbers().get(1);
        assertTrue(phone.getTypes().contains(TelephoneType.FAX));
        assertEquals("+1-800-MYFAX", phone.getText());

        // EMAIL
        assertEquals(2, c.getEmails().size());
        Email email = c.getEmails().get(0);
        assertTrue(email.getTypes().contains(EmailType.HOME));
        assertTrue(email.getTypes().contains(EmailType.PREF));
        assertNull(email.getPref());
        assertEquals("private@example.com", email.getValue());
        email = c.getEmails().get(1);
        assertTrue(email.getTypes().contains(EmailType.WORK));
        assertEquals("work@example.com", email.getValue());

        // ORG, TITLE, ROLE
        assertTrue(Arrays.equals(
                new String[]{"ABC, Inc.", "North American Division", "Marketing"},
                c.organization.getValues().toArray(new String[3])
        ));
        assertEquals("Director, Research and Development", c.jobTitle);
        assertEquals("Programmer", c.jobDescription);

        // IMPP
        assertEquals(3, c.getImpps().size());
        Impp impp = c.getImpps().get(0);
        assertTrue(impp.getTypes().contains(ImppType.PERSONAL));
        assertTrue(impp.getTypes().contains(ImppType.MOBILE));
        assertTrue(impp.getTypes().contains(ImppType.PREF));
        assertNull(impp.getPref());
        assertEquals("myIM", impp.getProtocol());
        assertEquals("anonymous@example.com", impp.getHandle());
        impp = c.getImpps().get(1);
        assertTrue(impp.getTypes().contains(ImppType.BUSINESS));
        assertEquals("skype", impp.getProtocol());
        assertEquals("echo@example.com", impp.getHandle());
        impp = c.getImpps().get(2);
        assertEquals("sip", impp.getProtocol());
        assertEquals("mysip@example.com", impp.getHandle());

        // NICKNAME
        assertTrue(Arrays.equals(new String[] { "Nick1", "Nick2" }, c.nickName.getValues().toArray()));

        // ADR
        assertEquals(2, c.getAddresses().size());
        Address addr = c.getAddresses().get(0);
        assertTrue(addr.getTypes().contains(AddressType.WORK));
        assertTrue(addr.getTypes().contains(AddressType.POSTAL));
        assertTrue(addr.getTypes().contains(AddressType.PARCEL));
        assertTrue(addr.getTypes().contains(AddressType.PREF));
        assertNull(addr.getPref());
        assertNull(addr.getPoBox());
        assertNull(addr.getExtendedAddress());
        assertEquals("6544 Battleford Drive", addr.getStreetAddress());
        assertEquals("Raleigh", addr.getLocality());
        assertEquals("NC", addr.getRegion());
        assertEquals("27613-3502", addr.getPostalCode());
        assertEquals("U.S.A.", addr.getCountry());
        addr = c.getAddresses().get(1);
        assertTrue(addr.getTypes().contains(AddressType.WORK));
        assertEquals("Postfach 314", addr.getPoBox());
        assertEquals("vorne hinten", addr.getExtendedAddress());
        assertEquals("Teststraße 22", addr.getStreetAddress());
        assertEquals("Mönchspfaffingen", addr.getLocality());
        assertNull(addr.getRegion());
        assertEquals("4043", addr.getPostalCode());
        assertEquals("Klöster-Reich", addr.getCountry());

        // NOTE
        assertEquals("This fax number is operational 0800 to 1715 EST, Mon-Fri.\n\n\nSecond note", c.note);

        // CATEGORIES
        assertTrue(Arrays.equals(
                new String[]{"A", "B'C"},
                c.getCategories().toArray()
        ));

        // URL
        assertEquals(2, c.getURLs().size());
        boolean url1 = false, url2 = false;
        for (Url url : c.getURLs()) {
            if ("https://davdroid.bitfire.at/".equals(url.getValue()) && url.getType() == null)
                url1 = true;
            if ("http://www.swbyps.restaurant.french/~chezchic.html".equals(url.getValue()) && "x-blog".equals(url.getType()))
                url2 = true;
        }
        assertTrue(url1 && url2);

        // BDAY
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        assertEquals("1996-04-15", dateFormat.format(c.birthDay.getDate()));
        // ANNIVERSARY
        assertEquals("2014-08-12", dateFormat.format(c.anniversary.getDate()));

        // RELATED
        assertEquals(2, c.getRelations().size());
        Related rel = c.getRelations().get(0);
        assertTrue(rel.getTypes().contains(RelatedType.CO_WORKER));
        assertTrue(rel.getTypes().contains(RelatedType.CRUSH));
        assertEquals("Ägidius", rel.getText());
        rel = c.getRelations().get(1);
        assertTrue(rel.getTypes().contains(RelatedType.PARENT));
        assertEquals("muuuum", rel.getText());

        // PHOTO
        @Cleanup InputStream photo = assetMgr.open("lol.jpg");
        assertTrue(Arrays.equals(IOUtils.toByteArray(photo), c.photo));
    }

    public void testVCard3FieldsAsVCard4() throws IOException {
        Contact c = regenerate(parseContact("allfields-vcard3.vcf", null), VCardVersion.V4_0);
        // let's check only things that should be different when VCard 4.0 is generated

        Telephone phone = c.getPhoneNumbers().get(0);
        assertFalse(phone.getTypes().contains(TelephoneType.PREF));
        assertNotNull(phone.getPref());

        Email email = c.getEmails().get(0);
        assertFalse(email.getTypes().contains(EmailType.PREF));
        assertNotNull(email.getPref());

        Impp impp = c.getImpps().get(0);
        assertFalse(impp.getTypes().contains(ImppType.PREF));
        assertNotNull(impp.getPref());

        Address addr = c.getAddresses().get(0);
        assertFalse(addr.getTypes().contains(AddressType.PREF));
        assertNotNull(addr.getPref());
    }

	private Contact parseContact(String fname, Charset charset) throws IOException {
		@Cleanup InputStream is = assetMgr.open(fname, AssetManager.ACCESS_STREAMING);
		return Contact.fromStream(is, charset, null)[0];
	}

	private Contact regenerate(Contact c, VCardVersion vCardVersion) throws IOException {
		return Contact.fromStream(new ByteArrayInputStream(c.toStream(vCardVersion).toByteArray()), null, null)[0];
	}


}

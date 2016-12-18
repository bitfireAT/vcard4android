/*
 * Copyright © Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.vcard4android;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;

import ezvcard.VCardVersion;
import ezvcard.parameter.AddressType;
import ezvcard.parameter.EmailType;
import ezvcard.parameter.ImppType;
import ezvcard.parameter.RelatedType;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.Address;
import ezvcard.property.Email;
import ezvcard.property.Impp;
import ezvcard.property.Nickname;
import ezvcard.property.Organization;
import ezvcard.property.Related;
import ezvcard.property.Telephone;
import ezvcard.property.Url;
import ezvcard.util.IOUtils;
import lombok.Cleanup;

import static org.junit.Assert.*;


public class ContactTest {

    private Contact parseContact(String fname, Charset charset) throws IOException {
        @Cleanup InputStream is = getClass().getClassLoader().getResourceAsStream(fname);
        assertNotNull(is);
        return Contact.fromStream(is, charset, null)[0];
    }

    private Contact regenerate(Contact c, VCardVersion vCardVersion) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        c.write(vCardVersion, GroupMethod.CATEGORIES, os);
        Constants.log.log(Level.INFO, "Re-generated VCard", os.toString());
        return Contact.fromStream(new ByteArrayInputStream(os.toByteArray()), null, null)[0];
    }

    private String toString(Contact c, GroupMethod groupMethod, VCardVersion vCardVersion) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        c.write(vCardVersion, groupMethod, os);
        return os.toString();
    }


    @Test
    public void testGenerateOrganizationOnly() throws IOException {
        Contact c = new Contact();
        c.uid = UUID.randomUUID().toString();
        c.organization = new Organization();
        c.organization.getValues().add("My Organization");
        c.organization.getValues().add("My Department");

        // vCard 3 needs FN and N
        String vCard = toString(c, GroupMethod.GROUP_VCARDS, VCardVersion.V3_0);
        assertTrue(vCard.contains("\nORG:My Organization;My Department\r\n"));
        assertTrue(vCard.contains("\nFN:My Organization\r\n"));
        assertTrue(vCard.contains("\nN:\r\n"));

        // vCard 4 only needs FN
        vCard = toString(c, GroupMethod.GROUP_VCARDS, VCardVersion.V4_0);
        assertTrue(vCard.contains("\nORG:My Organization;My Department\r\n"));
        assertTrue(vCard.contains("\nFN:My Organization\r\n"));
        assertFalse(vCard.contains("\nN:"));
    }

    @Test
    public void testGenerateOrgDepartmentOnly() throws IOException {
        Contact c = new Contact();
        c.uid = UUID.randomUUID().toString();
        c.organization = new Organization();
        c.organization.getValues().add("");
        c.organization.getValues().add("My Department");

        // vCard 3 needs FN and N
        String vCard = toString(c, GroupMethod.GROUP_VCARDS, VCardVersion.V3_0);
        assertTrue(vCard.contains("\nORG:;My Department\r\n"));
        assertTrue(vCard.contains("\nFN:My Department\r\n"));
        assertTrue(vCard.contains("\nN:\r\n"));

        // vCard 4 only needs FN
        vCard = toString(c, GroupMethod.GROUP_VCARDS, VCardVersion.V4_0);
        assertTrue(vCard.contains("\nORG:;My Department\r\n"));
        assertTrue(vCard.contains("\nFN:My Department\r\n"));
        assertFalse(vCard.contains("\nN:"));
    }

    @Test
    public void testGenerateGroup() throws IOException {
        Contact c = new Contact();
        c.uid = UUID.randomUUID().toString();
        c.displayName = "My Group";
        c.group = true;
        c.members.add("member1");
        c.members.add("member2");

        // vCard 3 needs FN and N
        // exception for Apple: "N:<group name>"
        String vCard = toString(c, GroupMethod.GROUP_VCARDS, VCardVersion.V3_0);
        assertTrue(vCard.contains("\nX-ADDRESSBOOKSERVER-KIND:group\r\n"));
        assertTrue(vCard.contains("\nFN:My Group\r\n"));
        assertTrue(vCard.contains("\nN:My Group\r\n"));
        assertTrue(vCard.contains("\nX-ADDRESSBOOKSERVER-MEMBER:urn:uuid:member1\r\n"));
        assertTrue(vCard.contains("\nX-ADDRESSBOOKSERVER-MEMBER:urn:uuid:member2\r\n"));

        // vCard 4 only needs FN
        vCard = toString(c, GroupMethod.GROUP_VCARDS, VCardVersion.V4_0);
        assertTrue(vCard.contains("\nKIND:group\r\n"));
        assertTrue(vCard.contains("\nFN:My Group\r\n"));
        assertFalse(vCard.contains("\nN:"));
        assertTrue(vCard.contains("\nMEMBER:urn:uuid:member1\r\n"));
        assertTrue(vCard.contains("\nMEMBER:urn:uuid:member2\r\n"));
    }

    @Test
    public void testGenerateWithoutName() throws IOException {
        /* no data */
        Contact c = new Contact();
        // vCard 3 needs FN and N
        String vCard = toString(c, GroupMethod.GROUP_VCARDS, VCardVersion.V3_0);
        assertTrue(vCard.contains("\nFN:\r\n"));
        assertTrue(vCard.contains("\nN:\r\n"));
        // vCard 4 only needs FN
        vCard = toString(c, GroupMethod.GROUP_VCARDS, VCardVersion.V4_0);
        assertTrue(vCard.contains("\nFN:\r\n"));
        assertFalse(vCard.contains("\nN:"));

        /* only UID */
        c.uid = UUID.randomUUID().toString();
        vCard = toString(c, GroupMethod.GROUP_VCARDS, VCardVersion.V3_0);
        // vCard 3 needs FN and N
        assertTrue(vCard.contains("\nFN:" + c.uid + "\r\n"));
        assertTrue(vCard.contains("\nN:\r\n"));
        // vCard 4 only needs FN
        vCard = toString(c, GroupMethod.GROUP_VCARDS, VCardVersion.V4_0);
        assertTrue(vCard.contains("\nFN:" + c.uid + "\r\n"));
        assertFalse(vCard.contains("\nN:"));

        // phone number available
        c.phoneNumbers.add(new LabeledProperty<>(new Telephone("12345")));
        assertTrue(toString(c, GroupMethod.GROUP_VCARDS, VCardVersion.V3_0).contains("\nFN:12345\r\n"));

        // email address available
        c.emails.add(new LabeledProperty<>(new Email("test@example.com")));
        assertTrue(toString(c, GroupMethod.GROUP_VCARDS, VCardVersion.V3_0).contains("\nFN:test@example.com\r\n"));

        // nick name available
        c.nickName = new Nickname();
        c.nickName.getValues().add("Nikki");
        assertTrue(toString(c, GroupMethod.GROUP_VCARDS, VCardVersion.V3_0).contains("\nFN:Nikki\r\n"));
    }

    @Test
    public void testGenerateLabeledProperty() throws IOException {
        Contact c = new Contact();
        c.uid = UUID.randomUUID().toString();
        c.phoneNumbers.add(new LabeledProperty<>(new Telephone("12345"), "My Phone"));
        String vCard = toString(c, GroupMethod.GROUP_VCARDS, VCardVersion.V3_0);
        assertTrue(vCard.contains("\ndavdroid1.TEL:12345\r\n"));
        assertTrue(vCard.contains("\ndavdroid1.X-ABLabel:My Phone\r\n"));

        c = regenerate(c, VCardVersion.V4_0);
        assertEquals("12345", c.phoneNumbers.get(0).property.getText());
        assertEquals("My Phone", c.phoneNumbers.get(0).label);
    }


    @Test
    public void testVCard3FieldsAsVCard3() throws IOException {
        Contact c = regenerate(parseContact("allfields-vcard3.vcf", null), VCardVersion.V3_0);

        // UID
        assertEquals("mostfields1@at.bitfire.vcard4android", c.uid);

        // FN
        assertEquals("Ämi Display", c.displayName);

        // N
        assertEquals("Firstname", c.givenName);
        assertEquals("Middlename1 Middlename2", c.middleName);
        assertEquals("Lastname", c.familyName);

        // phonetic names
        assertEquals("Förstnehm", c.phoneticGivenName);
        assertEquals("Mittelnehm", c.phoneticMiddleName);
        assertEquals("Laastnehm", c.phoneticFamilyName);

        // TEL
        assertEquals(2, c.phoneNumbers.size());
        LabeledProperty<Telephone> phone = c.phoneNumbers.get(0);
        assertEquals("Useless", phone.label);
        assertTrue(phone.property.getTypes().contains(TelephoneType.VOICE));
        assertTrue(phone.property.getTypes().contains(TelephoneType.HOME));
        assertTrue(phone.property.getTypes().contains(TelephoneType.PREF));
        assertNull(phone.property.getPref());
        assertEquals("+49 1234 56788", phone.property.getText());
        phone = c.phoneNumbers.get(1);
        assertNull(phone.label);
        assertTrue(phone.property.getTypes().contains(TelephoneType.FAX));
        assertEquals("+1-800-MYFAX", phone.property.getText());

        // EMAIL
        assertEquals(2, c.emails.size());
        LabeledProperty<Email> email = c.emails.get(0);
        assertNull(email.label);
        assertTrue(email.property.getTypes().contains(EmailType.HOME));
        assertTrue(email.property.getTypes().contains(EmailType.PREF));
        assertNull(email.property.getPref());
        assertEquals("private@example.com", email.property.getValue());
        email = c.emails.get(1);
        assertEquals("@work", email.label);
        assertTrue(email.property.getTypes().contains(EmailType.WORK));
        assertEquals("work@example.com", email.property.getValue());

        // ORG, TITLE, ROLE
        assertArrayEquals(
                new String[] { "ABC, Inc.", "North American Division", "Marketing" },
                c.organization.getValues().toArray(new String[3])
        );
        assertEquals("Director, Research and Development", c.jobTitle);
        assertEquals("Programmer", c.jobDescription);

        // IMPP
        assertEquals(3, c.impps.size());
        LabeledProperty<Impp> impp = c.impps.get(0);
        assertEquals("MyIM", impp.label);
        assertTrue(impp.property.getTypes().contains(ImppType.PERSONAL));
        assertTrue(impp.property.getTypes().contains(ImppType.MOBILE));
        assertTrue(impp.property.getTypes().contains(ImppType.PREF));
        assertNull(impp.property.getPref());
        assertEquals("myIM", impp.property.getProtocol());
        assertEquals("anonymous@example.com", impp.property.getHandle());
        impp = c.impps.get(1);
        assertNull(impp.label);
        assertTrue(impp.property.getTypes().contains(ImppType.BUSINESS));
        assertEquals("skype", impp.property.getProtocol());
        assertEquals("echo@example.com", impp.property.getHandle());
        impp = c.impps.get(2);
        assertNull(impp.label);
        assertEquals("sip", impp.property.getProtocol());
        assertEquals("mysip@example.com", impp.property.getHandle());

        // NICKNAME
        assertArrayEquals(
                new String[] { "Nick1", "Nick2" },
                c.nickName.getValues().toArray()
        );

        // ADR
        assertEquals(2, c.addresses.size());
        LabeledProperty<Address> addr = c.addresses.get(0);
        assertNull(addr.label);
        assertTrue(addr.property.getTypes().contains(AddressType.WORK));
        assertTrue(addr.property.getTypes().contains(AddressType.POSTAL));
        assertTrue(addr.property.getTypes().contains(AddressType.PARCEL));
        assertTrue(addr.property.getTypes().contains(AddressType.PREF));
        assertNull(addr.property.getPref());
        assertNull(addr.property.getPoBox());
        assertNull(addr.property.getExtendedAddress());
        assertEquals("6544 Battleford Drive", addr.property.getStreetAddress());
        assertEquals("Raleigh", addr.property.getLocality());
        assertEquals("NC", addr.property.getRegion());
        assertEquals("27613-3502", addr.property.getPostalCode());
        assertEquals("U.S.A.", addr.property.getCountry());
        addr = c.addresses.get(1);
        assertEquals("Monkey Tree", addr.label);
        assertTrue(addr.property.getTypes().contains(AddressType.WORK));
        assertEquals("Postfach 314", addr.property.getPoBox());
        assertEquals("vorne hinten", addr.property.getExtendedAddress());
        assertEquals("Teststraße 22", addr.property.getStreetAddress());
        assertEquals("Mönchspfaffingen", addr.property.getLocality());
        assertNull(addr.property.getRegion());
        assertEquals("4043", addr.property.getPostalCode());
        assertEquals("Klöster-Reich", addr.property.getCountry());

        // NOTE
        assertEquals("This fax number is operational 0800 to 1715 EST, Mon-Fri.\n\n\nSecond note", c.note);

        // CATEGORIES
        assertArrayEquals(
                new String[] { "A", "B'C" },
                c.categories.toArray()
        );

        // URL
        assertEquals(2, c.urls.size());
        boolean url1 = false, url2 = false;
        for (LabeledProperty<Url> url : c.urls) {
            if ("https://davdroid.bitfire.at/".equals(url.property.getValue()) && url.property.getType() == null && url.label == null)
                url1 = true;
            if ("http://www.swbyps.restaurant.french/~chezchic.html".equals(url.property.getValue()) && "x-blog".equals(url.property.getType()) && "blog".equals(url.label))
                url2 = true;
        }
        assertTrue(url1 && url2);

        // BDAY
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        assertEquals("1996-04-15", dateFormat.format(c.birthDay.getDate()));
        // ANNIVERSARY
        assertEquals("2014-08-12", dateFormat.format(c.anniversary.getDate()));

        // RELATED
        assertEquals(2, c.relations.size());
        Related rel = c.relations.get(0);
        assertTrue(rel.getTypes().contains(RelatedType.CO_WORKER));
        assertTrue(rel.getTypes().contains(RelatedType.CRUSH));
        assertEquals("Ägidius", rel.getText());
        rel = c.relations.get(1);
        assertTrue(rel.getTypes().contains(RelatedType.PARENT));
        assertEquals("muuuum", rel.getText());

        // PHOTO
        @Cleanup InputStream photo = getClass().getClassLoader().getResourceAsStream("lol.jpg");
        assertArrayEquals(IOUtils.toByteArray(photo), c.photo);
    }

    @Test
    public void testVCard3FieldsAsVCard4() throws IOException {
        Contact c = regenerate(parseContact("allfields-vcard3.vcf", null), VCardVersion.V4_0);
        // let's check only things that should be different when VCard 4.0 is generated

        Telephone phone = c.phoneNumbers.get(0).property;
        assertFalse(phone.getTypes().contains(TelephoneType.PREF));
        assertNotNull(phone.getPref());

        Email email = c.emails.get(0).property;
        assertFalse(email.getTypes().contains(EmailType.PREF));
        assertNotNull(email.getPref());

        Impp impp = c.impps.get(0).property;
        assertFalse(impp.getTypes().contains(ImppType.PREF));
        assertNotNull(impp.getPref());

        Address addr = c.addresses.get(0).property;
        assertFalse(addr.getTypes().contains(AddressType.PREF));
        assertNotNull(addr.getPref());
    }


    @Test
    public void testStrangeREV() throws IOException {
        Contact c = parseContact("strange-rev.vcf", null);
        assertTrue(c.unknownProperties.contains("REV;VALUE=timestamp:2016-11-19T17:44:22.057Z"));
    }

}

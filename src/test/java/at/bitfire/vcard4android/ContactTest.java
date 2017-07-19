/*
 * Copyright © Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.vcard4android;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import ezvcard.property.Birthday;
import ezvcard.property.Email;
import ezvcard.property.Impp;
import ezvcard.property.Nickname;
import ezvcard.property.Organization;
import ezvcard.property.Related;
import ezvcard.property.Telephone;
import ezvcard.property.Url;
import ezvcard.util.PartialDate;
import kotlin.text.Charsets;
import lombok.Cleanup;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class ContactTest {

    private Contact parseContact(String fname, Charset charset) throws IOException {
        @Cleanup InputStream is = getClass().getClassLoader().getResourceAsStream(fname);
        assertNotNull(is);
        return Contact.fromReader(new InputStreamReader(is, charset == null ? Charsets.UTF_8 : charset), null).get(0);
    }

    private Contact regenerate(Contact c, VCardVersion vCardVersion) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        c.write(vCardVersion, GroupMethod.CATEGORIES, os);
        Constants.log.log(Level.FINE, "Re-generated VCard", os.toString());
        return Contact.fromReader(new InputStreamReader(new ByteArrayInputStream(os.toByteArray()), Charsets.UTF_8), null).get(0);
    }

    private String toString(Contact c, GroupMethod groupMethod, VCardVersion vCardVersion) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        c.write(vCardVersion, groupMethod, os);
        return os.toString();
    }


    @Test
    public void testGenerateOrganizationOnly() throws IOException {
        Contact c = new Contact();
        c.setUid(UUID.randomUUID().toString());
        c.setOrganization(new Organization());
        c.getOrganization().getValues().add("My Organization");
        c.getOrganization().getValues().add("My Department");

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
        c.setUid(UUID.randomUUID().toString());
        c.setOrganization(new Organization());
        c.getOrganization().getValues().add("");
        c.getOrganization().getValues().add("My Department");

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
        c.setUid(UUID.randomUUID().toString());
        c.setDisplayName("My Group");
        c.setGroup(true);
        c.getMembers().add("member1");
        c.getMembers().add("member2");

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
        c.setUid(UUID.randomUUID().toString());
        vCard = toString(c, GroupMethod.GROUP_VCARDS, VCardVersion.V3_0);
        // vCard 3 needs FN and N
        assertTrue(vCard.contains("\nFN:" + c.getUid() + "\r\n"));
        assertTrue(vCard.contains("\nN:\r\n"));
        // vCard 4 only needs FN
        vCard = toString(c, GroupMethod.GROUP_VCARDS, VCardVersion.V4_0);
        assertTrue(vCard.contains("\nFN:" + c.getUid() + "\r\n"));
        assertFalse(vCard.contains("\nN:"));

        // phone number available
        c.getPhoneNumbers().add(new LabeledProperty<>(new Telephone("12345")));
        assertTrue(toString(c, GroupMethod.GROUP_VCARDS, VCardVersion.V3_0).contains("\nFN:12345\r\n"));

        // email address available
        c.getEmails().add(new LabeledProperty<>(new Email("test@example.com")));
        assertTrue(toString(c, GroupMethod.GROUP_VCARDS, VCardVersion.V3_0).contains("\nFN:test@example.com\r\n"));

        // nick name available
        c.setNickName(new Nickname());
        c.getNickName().getValues().add("Nikki");
        assertTrue(toString(c, GroupMethod.GROUP_VCARDS, VCardVersion.V3_0).contains("\nFN:Nikki\r\n"));
    }

    @Test
    public void testGenerateLabeledProperty() throws IOException {
        Contact c = new Contact();
        c.setUid(UUID.randomUUID().toString());
        c.getPhoneNumbers().add(new LabeledProperty<>(new Telephone("12345"), "My Phone"));
        String vCard = toString(c, GroupMethod.GROUP_VCARDS, VCardVersion.V3_0);
        assertTrue(vCard.contains("\ndavdroid1.TEL:12345\r\n"));
        assertTrue(vCard.contains("\ndavdroid1.X-ABLabel:My Phone\r\n"));

        c = regenerate(c, VCardVersion.V4_0);
        assertEquals("12345", c.getPhoneNumbers().get(0).getProperty().getText());
        assertEquals("My Phone", c.getPhoneNumbers().get(0).getLabel());
    }


    @Test
    public void testVCard3FieldsAsVCard3() throws IOException {
        Contact c = regenerate(parseContact("allfields-vcard3.vcf", null), VCardVersion.V3_0);

        // UID
        assertEquals("mostfields1@at.bitfire.vcard4android", c.getUid());

        // FN
        assertEquals("Ämi Display", c.getDisplayName());

        // N
        assertEquals("Firstname", c.getGivenName());
        assertEquals("Middlename1 Middlename2", c.getMiddleName());
        assertEquals("Lastname", c.getFamilyName());

        // phonetic names
        assertEquals("Förstnehm", c.getPhoneticGivenName());
        assertEquals("Mittelnehm", c.getPhoneticMiddleName());
        assertEquals("Laastnehm", c.getPhoneticFamilyName());

        // TEL
        assertEquals(2, c.getPhoneNumbers().size());
        LabeledProperty<Telephone> phone = c.getPhoneNumbers().get(0);
        assertEquals("Useless", phone.getLabel());
        assertTrue(phone.getProperty().getTypes().contains(TelephoneType.VOICE));
        assertTrue(phone.getProperty().getTypes().contains(TelephoneType.HOME));
        assertTrue(phone.getProperty().getTypes().contains(TelephoneType.PREF));
        assertNull(phone.getProperty().getPref());
        assertEquals("+49 1234 56788", phone.getProperty().getText());
        phone = c.getPhoneNumbers().get(1);
        assertNull(phone.getLabel());
        assertTrue(phone.getProperty().getTypes().contains(TelephoneType.FAX));
        assertEquals("+1-800-MYFAX", phone.getProperty().getText());

        // EMAIL
        assertEquals(2, c.getEmails().size());
        LabeledProperty<Email> email = c.getEmails().get(0);
        assertNull(email.getLabel());
        assertTrue(email.getProperty().getTypes().contains(EmailType.HOME));
        assertTrue(email.getProperty().getTypes().contains(EmailType.PREF));
        assertNull(email.getProperty().getPref());
        assertEquals("private@example.com", email.getProperty().getValue());
        email = c.getEmails().get(1);
        assertEquals("@work", email.getLabel());
        assertTrue(email.getProperty().getTypes().contains(EmailType.WORK));
        assertEquals("work@example.com", email.getProperty().getValue());

        // ORG, TITLE, ROLE
        assertArrayEquals(
                new String[] { "ABC, Inc.", "North American Division", "Marketing" },
                c.getOrganization().getValues().toArray(new String[3])
        );
        assertEquals("Director, Research and Development", c.getJobTitle());
        assertEquals("Programmer", c.getJobDescription());

        // IMPP
        assertEquals(3, c.getImpps().size());
        LabeledProperty<Impp> impp = c.getImpps().get(0);
        assertEquals("MyIM", impp.getLabel());
        assertTrue(impp.getProperty().getTypes().contains(ImppType.PERSONAL));
        assertTrue(impp.getProperty().getTypes().contains(ImppType.MOBILE));
        assertTrue(impp.getProperty().getTypes().contains(ImppType.PREF));
        assertNull(impp.getProperty().getPref());
        assertEquals("myIM", impp.getProperty().getProtocol());
        assertEquals("anonymous@example.com", impp.getProperty().getHandle());
        impp = c.getImpps().get(1);
        assertNull(impp.getLabel());
        assertTrue(impp.getProperty().getTypes().contains(ImppType.BUSINESS));
        assertEquals("skype", impp.getProperty().getProtocol());
        assertEquals("echo@example.com", impp.getProperty().getHandle());
        impp = c.getImpps().get(2);
        assertNull(impp.getLabel());
        assertEquals("sip", impp.getProperty().getProtocol());
        assertEquals("mysip@example.com", impp.getProperty().getHandle());

        // NICKNAME
        assertArrayEquals(
                new String[] { "Nick1", "Nick2" },
                c.getNickName().getValues().toArray()
        );

        // ADR
        assertEquals(2, c.getAddresses().size());
        LabeledProperty<Address> addr = c.getAddresses().get(0);
        assertNull(addr.getLabel());
        assertTrue(addr.getProperty().getTypes().contains(AddressType.WORK));
        assertTrue(addr.getProperty().getTypes().contains(AddressType.POSTAL));
        assertTrue(addr.getProperty().getTypes().contains(AddressType.PARCEL));
        assertTrue(addr.getProperty().getTypes().contains(AddressType.PREF));
        assertNull(addr.getProperty().getPref());
        assertNull(addr.getProperty().getPoBox());
        assertNull(addr.getProperty().getExtendedAddress());
        assertEquals("6544 Battleford Drive", addr.getProperty().getStreetAddress());
        assertEquals("Raleigh", addr.getProperty().getLocality());
        assertEquals("NC", addr.getProperty().getRegion());
        assertEquals("27613-3502", addr.getProperty().getPostalCode());
        assertEquals("U.S.A.", addr.getProperty().getCountry());
        addr = c.getAddresses().get(1);
        assertEquals("Monkey Tree", addr.getLabel());
        assertTrue(addr.getProperty().getTypes().contains(AddressType.WORK));
        assertEquals("Postfach 314", addr.getProperty().getPoBox());
        assertEquals("vorne hinten", addr.getProperty().getExtendedAddress());
        assertEquals("Teststraße 22", addr.getProperty().getStreetAddress());
        assertEquals("Mönchspfaffingen", addr.getProperty().getLocality());
        assertNull(addr.getProperty().getRegion());
        assertEquals("4043", addr.getProperty().getPostalCode());
        assertEquals("Klöster-Reich", addr.getProperty().getCountry());

        // NOTE
        assertEquals("This fax number is operational 0800 to 1715 EST, Mon-Fri.\n\n\nSecond note", c.getNote());

        // CATEGORIES
        assertArrayEquals(
                new String[] { "A", "B'C" },
                c.getCategories().toArray()
        );

        // URL
        assertEquals(2, c.getUrls().size());
        boolean url1 = false, url2 = false;
        for (LabeledProperty<Url> url : c.getUrls()) {
            if ("https://davdroid.bitfire.at/".equals(url.getProperty().getValue()) && url.getProperty().getType() == null && url.getLabel() == null)
                url1 = true;
            if ("http://www.swbyps.restaurant.french/~chezchic.html".equals(url.getProperty().getValue()) && "x-blog".equals(url.getProperty().getType()) && "blog".equals(url.getLabel()))
                url2 = true;
        }
        assertTrue(url1 && url2);

        // BDAY
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        assertEquals("1996-04-15", dateFormat.format(c.getBirthDay().getDate()));
        // ANNIVERSARY
        assertEquals("2014-08-12", dateFormat.format(c.getAnniversary().getDate()));

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
        @Cleanup InputStream photo = getClass().getClassLoader().getResourceAsStream("lol.jpg");
        assertArrayEquals(IOUtils.toByteArray(photo), c.getPhoto());
    }

    @Test
    public void testVCard3FieldsAsVCard4() throws IOException {
        Contact c = regenerate(parseContact("allfields-vcard3.vcf", null), VCardVersion.V4_0);
        // let's check only things that should be different when VCard 4.0 is generated

        Telephone phone = c.getPhoneNumbers().get(0).getProperty();
        assertFalse(phone.getTypes().contains(TelephoneType.PREF));
        assertNotNull(phone.getPref());

        Email email = c.getEmails().get(0).getProperty();
        assertFalse(email.getTypes().contains(EmailType.PREF));
        assertNotNull(email.getPref());

        Impp impp = c.getImpps().get(0).getProperty();
        assertFalse(impp.getTypes().contains(ImppType.PREF));
        assertNotNull(impp.getPref());

        Address addr = c.getAddresses().get(0).getProperty();
        assertFalse(addr.getTypes().contains(AddressType.PREF));
        assertNotNull(addr.getPref());
    }

    @Test
    public void testVCard4FieldsAsVCard3() throws IOException {
        Contact c = regenerate(parseContact("vcard4.vcf", null), VCardVersion.V3_0);;
        assertEquals(new Birthday(PartialDate.parse("--04-16")), c.getBirthDay());
    }

    @Test
    public void testVCard4FieldsAsVCard4() throws IOException {
        Contact c = regenerate(parseContact("vcard4.vcf", null), VCardVersion.V4_0);
        assertEquals(new Birthday(PartialDate.parse("--04-16")), c.getBirthDay());
    }


    @Test
    public void testStrangeREV() throws IOException {
        Contact c = parseContact("strange-rev.vcf", null);
        assertNull(c.getUnknownProperties());
    }

}

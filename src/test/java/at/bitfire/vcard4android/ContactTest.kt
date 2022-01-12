/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android

import ezvcard.VCardVersion
import ezvcard.parameter.*
import ezvcard.property.Birthday
import ezvcard.util.PartialDate
import org.apache.commons.io.IOUtils
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class ContactTest {

    private fun parseContact(fname: String, charset: Charset = Charsets.UTF_8) =
            javaClass.classLoader!!.getResourceAsStream(fname).use { stream ->
                Contact.fromReader(InputStreamReader(stream, charset), false, null).first()
            }

    private fun regenerate(c: Contact, vCardVersion: VCardVersion): Contact {
        val os = ByteArrayOutputStream()
        c.writeVCard(vCardVersion, os)
        return Contact.fromReader(InputStreamReader(ByteArrayInputStream(os.toByteArray()), Charsets.UTF_8), false,null).first()
    }

    private fun toString(c: Contact, groupMethod: GroupMethod, vCardVersion: VCardVersion): String {
        val os = ByteArrayOutputStream()
        c.writeVCard(vCardVersion, os)
        return os.toString()
    }


    @Test
    fun testVCard3FieldsAsVCard3() {
        val c = regenerate(parseContact("allfields-vcard3.vcf"), VCardVersion.V3_0)

        // UID
        assertEquals("mostfields1@at.bitfire.vcard4android", c.uid)

        // FN
        assertEquals("Ämi Display", c.displayName)

        // N
        assertEquals("Firstname", c.givenName)
        assertEquals("Middlename1 Middlename2", c.middleName)
        assertEquals("Lastname", c.familyName)
        assertEquals("Förstnehm", c.phoneticGivenName)
        assertEquals("Mittelnehm", c.phoneticMiddleName)
        assertEquals("Laastnehm", c.phoneticFamilyName)

        // phonetic names
        assertEquals("Förstnehm", c.phoneticGivenName)
        assertEquals("Mittelnehm", c.phoneticMiddleName)
        assertEquals("Laastnehm", c.phoneticFamilyName)

        // TEL
        assertEquals(2, c.phoneNumbers.size)
        var phone = c.phoneNumbers.first
        assertEquals("Useless", phone.label)
        assertTrue(phone.property.types.contains(TelephoneType.VOICE))
        assertTrue(phone.property.types.contains(TelephoneType.HOME))
        assertTrue(phone.property.types.contains(TelephoneType.PREF))
        assertNull(phone.property.pref)
        assertEquals("+49 1234 56788", phone.property.text)
        phone = c.phoneNumbers[1]
        assertNull(phone.label)
        assertTrue(phone.property.types.contains(TelephoneType.FAX))
        assertEquals("+1-800-MYFAX", phone.property.text)

        // EMAIL
        assertEquals(2, c.emails.size)
        var email = c.emails.first
        assertNull(email.label)
        assertTrue(email.property.types.contains(EmailType.HOME))
        assertTrue(email.property.types.contains(EmailType.PREF))
        assertNull(email.property.pref)
        assertEquals("private@example.com", email.property.value)
        email = c.emails[1]
        assertEquals("@work", email.label)
        assertTrue(email.property.types.contains(EmailType.WORK))
        assertEquals("work@example.com", email.property.value)

        // ORG, TITLE, ROLE
        assertEquals(
                listOf("ABC, Inc.", "North American Division", "Marketing"),
                c.organization!!.values
        )
        assertEquals("Director, Research and Development", c.jobTitle)
        assertEquals("Programmer", c.jobDescription)

        // IMPP
        assertEquals(3, c.impps.size)
        var impp = c.impps.first
        assertEquals("MyIM", impp.label)
        assertTrue(impp.property.types.contains(ImppType.PERSONAL))
        assertTrue(impp.property.types.contains(ImppType.MOBILE))
        assertTrue(impp.property.types.contains(ImppType.PREF))
        assertNull(impp.property.pref)
        assertEquals("myIM", impp.property.protocol)
        assertEquals("anonymous@example.com", impp.property.handle)
        impp = c.impps[1]
        assertNull(impp.label)
        assertTrue(impp.property.types.contains(ImppType.BUSINESS))
        assertEquals("skype", impp.property.protocol)
        assertEquals("echo@example.com", impp.property.handle)
        impp = c.impps[2]
        assertNull(impp.label)
        assertEquals("sip", impp.property.protocol)
        assertEquals("mysip@example.com", impp.property.handle)

        // NICKNAME
        assertEquals(
                listOf("Nick1", "Nick2"),
                c.nickName!!.property.values
        )

        // ADR
        assertEquals(2, c.addresses.size)
        var addr = c.addresses.first
        assertNull(addr.label)
        assertTrue(addr.property.types.contains(AddressType.WORK))
        assertTrue(addr.property.types.contains(AddressType.POSTAL))
        assertTrue(addr.property.types.contains(AddressType.PARCEL))
        assertTrue(addr.property.types.contains(AddressType.PREF))
        assertNull(addr.property.pref)
        assertNull(addr.property.poBox)
        assertNull(addr.property.extendedAddress)
        assertEquals("6544 Battleford Drive", addr.property.streetAddress)
        assertEquals("Raleigh", addr.property.locality)
        assertEquals("NC", addr.property.region)
        assertEquals("27613-3502", addr.property.postalCode)
        assertEquals("U.S.A.", addr.property.country)
        addr = c.addresses[1]
        assertEquals("Monkey Tree", addr.label)
        assertTrue(addr.property.types.contains(AddressType.WORK))
        assertEquals("Postfach 314", addr.property.poBox)
        assertEquals("vorne hinten", addr.property.extendedAddress)
        assertEquals("Teststraße 22", addr.property.streetAddress)
        assertEquals("Mönchspfaffingen", addr.property.locality)
        assertNull(addr.property.region)
        assertEquals("4043", addr.property.postalCode)
        assertEquals("Klöster-Reich", addr.property.country)
        assertEquals("BEGIN:VCARD\r\n" +
                "VERSION:3.0\r\n" +
                "X-TEST;A=B:Value\r\n" +
                "END:VCARD\r\n", c.unknownProperties)

        // NOTE
        assertEquals("This fax number is operational 0800 to 1715 EST, Mon-Fri.\n\n\nSecond note", c.note)

        // CATEGORIES
        assertEquals(
                listOf("A", "B'C"),
                c.categories
        )

        // URL
        assertEquals(2, c.urls.size)
        var url1 = false
        var url2 = false
        for (url in c.urls) {
            if ("https://www.davx5.com/" == url.property.value && url.property.type == null && url.label == null)
                url1 = true
            if ("http://www.swbyps.restaurant.french/~chezchic.html" == url.property.value && "x-blog" == url.property.type && "blog" == url.label)
                url2 = true
        }
        assertTrue(url1 && url2)

        // BDAY
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        assertEquals("1996-04-15", dateFormat.format(c.birthDay!!.date))
        // ANNIVERSARY
        assertEquals("2014-08-12", dateFormat.format(c.anniversary!!.date))
        // X-ABDATE
        assertEquals(1, c.customDates.size)
        c.customDates.first.also { date ->
            assertEquals("Custom Date", date.label)
            assertEquals(ZonedDateTime.of(2021, 7, 29, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant(), date.property.date.toInstant())
        }

        // RELATED
        assertEquals(2, c.relations.size)
        var rel = c.relations.first
        assertTrue(rel.types.contains(RelatedType.CO_WORKER))
        assertTrue(rel.types.contains(RelatedType.CRUSH))
        assertEquals("Ägidius", rel.text)
        rel = c.relations[1]
        assertTrue(rel.types.contains(RelatedType.PARENT))
        assertEquals("muuum@example.com", rel.text)

        // PHOTO
        javaClass.classLoader!!.getResourceAsStream("lol.jpg").use { photo ->
            assertArrayEquals(IOUtils.toByteArray(photo), c.photo)
        }
    }

    @Test
    fun testVCard3FieldsAsVCard4() {
        val c = regenerate(parseContact("allfields-vcard3.vcf"), VCardVersion.V4_0)
        // let's check only things that should be different when VCard 4.0 is generated

        val phone = c.phoneNumbers.first.property
        assertFalse(phone.types.contains(TelephoneType.PREF))
        assertNotNull(phone.pref)

        val email = c.emails.first.property
        assertFalse(email.types.contains(EmailType.PREF))
        assertNotNull(email.pref)

        val impp = c.impps.first.property
        assertFalse(impp.types.contains(ImppType.PREF))
        assertNotNull(impp.pref)

        val addr = c.addresses.first.property
        assertFalse(addr.types.contains(AddressType.PREF))
        assertNotNull(addr.pref)
    }

    @Test
    fun testVCard4FieldsAsVCard3() {
        val c = regenerate(parseContact("vcard4.vcf"), VCardVersion.V3_0)
        assertEquals(Birthday(PartialDate.parse("--04-16")), c.birthDay)
    }

    @Test
    fun testVCard4FieldsAsVCard4() {
        val c = regenerate(parseContact("vcard4.vcf"), VCardVersion.V4_0)
        assertEquals(Birthday(PartialDate.parse("--04-16")), c.birthDay)
    }


    @Test
    fun testStrangeREV() {
        val c = parseContact("strange-rev.vcf")
        assertNull(c.unknownProperties)
    }

}

/*
 * Copyright © Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.vcard4android

import android.Manifest
import android.accounts.Account
import android.content.ContentProviderClient
import android.provider.ContactsContract
import android.util.Base64
import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import at.bitfire.vcard4android.impl.TestAddressBook
import ezvcard.VCardVersion
import ezvcard.parameter.EmailType
import ezvcard.property.Address
import ezvcard.property.Birthday
import ezvcard.property.Email
import ezvcard.util.PartialDate
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.text.SimpleDateFormat

class AndroidContactTest {

    @JvmField
    @Rule
    val permissionRule = GrantPermissionRule.grant(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)!!

    private val testAccount = Account("AndroidContactTest", "at.bitfire.vcard4android")
    
    private lateinit var provider: ContentProviderClient
    private lateinit var addressBook: TestAddressBook

    @Before
    fun connect() {
        val context = InstrumentationRegistry.getInstrumentation().context
        provider = context.contentResolver.acquireContentProviderClient(ContactsContract.AUTHORITY)!!
        assertNotNull(provider)

        addressBook = TestAddressBook(testAccount, provider)
    }

    @After
    fun disconnect() {
        @Suppress("DEPRECATION")
        provider.release()
    }


    @Test
    @SmallTest
    fun testAddAndReadContact() {
        val samplePhoto = Base64.decode("/9j/4AAQSkZJRgABAQEASABIAAD//gATQ3JlYXRlZCB3aXRoIEdJTVD/2wBDAAMCAgMCAgMDAwMEAwMEBQgFBQQEBQoHBwYIDAoMDAsKCwsNDhIQDQ4RDgsLEBYQERMUFRUVDA8XGBYUGBIUFRT/2wBDAQMEBAUEBQkFBQkUDQsNFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBT/wgARCAAFAAUDAREAAhEBAxEB/8QAFAABAAAAAAAAAAAAAAAAAAAACP/EABQBAQAAAAAAAAAAAAAAAAAAAAD/2gAMAwEAAhADEAAAAVSf/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABBQJ//8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/aAAgBAwEBPwF//8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/aAAgBAgEBPwF//8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQAGPwJ//8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPyF//9oADAMBAAIAAwAAABCf/8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/aAAgBAwEBPxB//8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/aAAgBAgEBPxB//8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxB//9k=", Base64.DEFAULT)

        val vcard = Contact()
        vcard.displayName = "Mya Contact"
        vcard.prefix = "Magª"
        vcard.givenName = "Mya"
        vcard.familyName = "Contact"
        vcard.suffix = "BSc"
        vcard.phoneticGivenName = "Först"
        vcard.phoneticMiddleName = "Mittelerde"
        vcard.phoneticFamilyName = "Fämilie"
        vcard.birthDay = Birthday(SimpleDateFormat("yyyy-MM-dd").parse("1980-04-16"))
        vcard.photo = samplePhoto

        val contact = AndroidContact(addressBook, vcard, null, null)
        contact.add()

        val contact2 = addressBook.findContactByID(contact.id!!)
        try {
            val vcard2 = contact2.contact!!
            assertEquals(vcard.displayName, vcard2.displayName)
            assertEquals(vcard.prefix, vcard2.prefix)
            assertEquals(vcard.givenName, vcard2.givenName)
            assertEquals(vcard.familyName, vcard2.familyName)
            assertEquals(vcard.suffix, vcard2.suffix)
            assertEquals(vcard.phoneticGivenName, vcard2.phoneticGivenName)
            assertEquals(vcard.phoneticMiddleName, vcard2.phoneticMiddleName)
            assertEquals(vcard.phoneticFamilyName, vcard2.phoneticFamilyName)
            assertEquals(vcard.birthDay, vcard2.birthDay)
            assertNotNull(vcard.photo)
        } finally {
            contact2.delete()
        }
    }

    @Test
    @SmallTest
    fun testInvalidPREF() {
        val vCard = "BEGIN:VCARD\r\n" +
                "VERSION:4.0\r\n" +
                "FN:Test\r\n" +
                "TEL;CELL=;PREF=:+12345\r\n" +
                "EMAIL;PREF=invalid:test@example.com\r\n" +
                "END:VCARD\r\n"
        val contacts = Contact.fromReader(StringReader(vCard), null)

        val dbContact = AndroidContact(addressBook, contacts.first(), null, null)
        dbContact.add()

        val dbContact2 = addressBook.findContactByID(dbContact.id!!)
        try {
            val contact2 = dbContact2.contact!!
            assertEquals("Test", contact2.displayName)
            assertEquals("+12345", contact2.phoneNumbers.first.property.text)
            assertEquals("test@example.com", contact2.emails.first.property.value)
        } finally {
            dbContact2.delete()
        }
    }

    @Test
    @MediumTest
    fun testLargeTransactionManyRows() {
        val vcard = Contact()
        vcard.displayName = "Large Transaction (many rows)"
        for (i in 0 until 4000)
            vcard.emails += LabeledProperty(Email("test$i@example.com"))

        val contact = AndroidContact(addressBook, vcard, null, null)
        contact.add()

        val contact2 = addressBook.findContactByID(contact.id!!)
        try {
            val vcard2 = contact2.contact!!
            assertEquals(4000, vcard2.emails.size)
        } finally {
            contact2.delete()
        }
    }

    @Test(expected = ContactsStorageException::class)
    fun testLargeTransactionSingleRow() {
        val vcard = Contact()
        vcard.displayName = "Large Transaction (one row which is too large)"

        // 1 MB eTag ... have fun
        val data = CharArray(1024*1024) { 'x' }
        val eTag = String(data)

        val contact = AndroidContact(addressBook, vcard, null, eTag)
        contact.add()
    }

    @Test
    fun testAddressCaretEncoding() {
        val address = Address()
        address.label = "My \"Label\"\nLine 2"
        address.streetAddress = "Street \"Address\""
        val contact = Contact()
        contact.addresses += LabeledProperty(address)

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

        val os = ByteArrayOutputStream()
        contact.write(VCardVersion.V4_0, GroupMethod.GROUP_VCARDS, os)
        Constants.log.info(os.toString())
        assertTrue(os.toString().contains("ADR;LABEL=My ^'Label^'\\nLine 2:;;Street \"Address\";;;;"))
    }

    @Test
    fun testBirthdayWithoutYear() {
        val vcard = Contact()
        vcard.displayName = "Mya Contact"
        vcard.birthDay = Birthday(PartialDate.parse("-04-16"))

        val contact = AndroidContact(addressBook, vcard, null, null)
        contact.add()

        val contact2 = addressBook.findContactByID(contact.id!!)
        try {
            val vcard2 = contact2.contact!!
            assertEquals(vcard.displayName, vcard2.displayName)
            assertEquals(vcard.birthDay, vcard2.birthDay)
        } finally {
            contact2.delete()
        }
    }

    @Test
    fun testEmailTypes() {
        val vCard = "BEGIN:VCARD\r\n" +
                "VERSION:4.0\r\n" +
                "FN:Test\r\n" +
                "EMAIL;TYPE=internet;TYPE=work:work@example.com\r\n" +
                "EMAIL;TYPE=HOME:home@example.com\r\n" +
                "EMAIL;TYPE=internet,pref:other1@example.com\r\n" +
                "EMAIL;TYPE=x400,oTHer:other2@example.com\r\n" +
                "EMAIL;TYPE=x-mobile:mobile@example.com\r\n" +
                "group1.EMAIL;TYPE=x-with-label:withlabel@example.com\r\n" +
                "group1.X-ABLABEL:With label and x-type\r\n" +
                "END:VCARD\r\n"
        val contacts = Contact.fromReader(StringReader(vCard), null)

        val dbContact = AndroidContact(addressBook, contacts.first(), null, null)
        dbContact.add()

        val dbContact2 = addressBook.findContactByID(dbContact.id!!)
        try {
            val contact2 = dbContact2.contact!!
            assertEquals(6, contact2.emails.size)

            contact2.emails[0].property.let { email ->
                assertEquals("work@example.com", email.value)
                assertArrayEquals(arrayOf(EmailType.WORK), email.types.toTypedArray())
                assertNull(email.pref)
            }

            contact2.emails[1].property.let { email ->
                assertEquals("home@example.com", email.value)
                assertArrayEquals(arrayOf(EmailType.HOME), email.types.toTypedArray())
                assertNull(email.pref)
            }

            contact2.emails[2].property.let { email ->
                assertEquals("other1@example.com", email.value)
                assertTrue(email.types.isEmpty())
                assertNotEquals(0, email.pref)
            }

            contact2.emails[3].property.let { email ->
                assertEquals("other2@example.com", email.value)
                assertTrue(email.types.isEmpty())
                assertNull(email.pref)
            }

            contact2.emails[4].property.let { email ->
                assertEquals("mobile@example.com", email.value)
                assertArrayEquals(arrayOf(Contact.EMAIL_TYPE_MOBILE), email.types.toTypedArray())
                assertNull(email.pref)
            }

            contact2.emails[5].let { email ->
                assertEquals("withlabel@example.com", email.property.value)
                assertEquals(EmailType.get("x-with-label-and-x-type"), email.property.types.first())
                assertEquals("With label and x-type", email.label)
            }
        } finally {
            dbContact2.delete()
        }
    }

    @Test
    fun testWebsiteTypes() {
        val vCard = "BEGIN:VCARD\r\n" +
                "VERSION:4.0\r\n" +
                "FN:Test\r\n" +
                "URL;TYPE=home:http://home.example\r\n" +
                "URL;TYPE=WORK:http://WORK.example\r\n" +
                "URL;TYPE=Custom:http://Custom.example\r\n" +
                "group1.URL;TYPE=x-custom-with-label:http://Custom.example\r\n" +
                "group1.X-ABLABEL:Custom (with label)\r\n" +
                "END:VCARD\r\n"
        val contacts = Contact.fromReader(StringReader(vCard), null)

        val dbContact = AndroidContact(addressBook, contacts.first(), null, null)
        dbContact.add()

        val dbContact2 = addressBook.findContactByID(dbContact.id!!)
        try {
            val contact2 = dbContact2.contact!!
            assertEquals(4, contact2.urls.size)

            contact2.urls[0].property.let { url ->
                assertEquals("http://home.example", url.value)
                assertEquals("home", url.type)
            }

            contact2.urls[1].property.let { url ->
                assertEquals("http://WORK.example", url.value)
                assertEquals("work", url.type)
            }

            contact2.urls[2].property.let { url ->
                assertEquals("http://Custom.example", url.value)
                assertEquals("x-custom", url.type)
            }

            contact2.urls[3].let { url ->
                assertEquals("http://Custom.example", url.property.value)
                assertEquals("x-custom-with-label", url.property.type)
                assertEquals("Custom (with label)", url.label)
            }
        } finally {
            dbContact2.delete()
        }
    }


    @Test
    fun testLabelToXName() {
        assertEquals("x-aunties-home", AndroidContact.labelToXName("auntie's home"))
    }

    @Test
    fun testToURIScheme() {
        assertEquals("testp+csfgh-ewt4345.2qiuz4", AndroidContact.toURIScheme("02 34test#ä{☺}ö p[]ß+csfgh()-e_wt4\\345.2qiuz4"))
        assertEquals("CyanogenModForum", AndroidContact.toURIScheme("CyanogenMod Forum"))
        assertEquals("CyanogenModForum", AndroidContact.toURIScheme("CyanogenMod_Forum"))
    }

    @Test
    fun testXNameToLabel() {
        assertEquals("Aunties Home", AndroidContact.xNameToLabel("X-AUNTIES-HOME"))
        assertEquals("Aunties Home", AndroidContact.xNameToLabel("X-AUNTIES_HOME"))
    }

}

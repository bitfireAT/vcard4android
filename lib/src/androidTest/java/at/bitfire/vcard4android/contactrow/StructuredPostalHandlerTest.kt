/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.content.ContentValues
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import at.bitfire.vcard4android.Contact
import ezvcard.parameter.AddressType
import org.junit.Assert.*
import org.junit.Test

class StructuredPostalHandlerTest {

    @Test
    fun testEmpty() {
        val contact = Contact()
        StructuredPostalHandler.handle(ContentValues().apply {
            putNull(StructuredPostal.STREET)
        }, contact)
        assertTrue(contact.addresses.isEmpty())
    }


    @Test
    fun testValues() {
        val contact = Contact()
        StructuredPostalHandler.handle(ContentValues().apply {
            put(StructuredPostal.STREET, "Street 1\n(Corner Street 2)")
            put(StructuredPostal.POBOX, "PO Box 123")
            put(StructuredPostal.NEIGHBORHOOD, "Near the park")
            put(StructuredPostal.CITY, "Sampletown")
            put(StructuredPostal.REGION, "Sampleregion")
            put(StructuredPostal.POSTCODE, "ZIP")
            put(StructuredPostal.COUNTRY, "Samplecountry")

            put(StructuredPostal.FORMATTED_ADDRESS, "Full Formatted Address")
        }, contact)
        assertArrayEquals(arrayOf("Street 1", "(Corner Street 2)"), contact.addresses[0].property.streetAddresses.toTypedArray())
        assertArrayEquals(arrayOf("PO Box 123"), contact.addresses[0].property.poBoxes.toTypedArray())
        assertArrayEquals(arrayOf("Near the park"), contact.addresses[0].property.extendedAddresses.toTypedArray())
        assertArrayEquals(arrayOf("PO Box 123"), contact.addresses[0].property.poBoxes.toTypedArray())
        assertArrayEquals(arrayOf("Sampletown"), contact.addresses[0].property.localities.toTypedArray())
        assertArrayEquals(arrayOf("Sampleregion"), contact.addresses[0].property.regions.toTypedArray())
        assertArrayEquals(arrayOf("ZIP"), contact.addresses[0].property.postalCodes.toTypedArray())
        assertArrayEquals(arrayOf("Samplecountry"), contact.addresses[0].property.countries.toTypedArray())
        assertNull(contact.addresses[0].property.label)
    }


    @Test
    fun testType_CustomNoLabel() {
        val contact = Contact()
        StructuredPostalHandler.handle(ContentValues().apply {
            put(StructuredPostal.STREET, "Street")
            put(StructuredPostal.TYPE, StructuredPostal.TYPE_CUSTOM)
        }, contact)
        assertTrue(contact.addresses[0].property.types.isEmpty())
    }

    @Test
    fun testType_CustomWithLabel() {
        val contact = Contact()
        StructuredPostalHandler.handle(ContentValues().apply {
            put(StructuredPostal.STREET, "Street")
            put(StructuredPostal.TYPE, StructuredPostal.TYPE_CUSTOM)
            put(StructuredPostal.LABEL, "Label")
        }, contact)
        assertTrue(contact.addresses[0].property.types.isEmpty())
        assertEquals("Label", contact.addresses[0].label)
    }

    @Test
    fun testType_Home() {
        val contact = Contact()
        StructuredPostalHandler.handle(ContentValues().apply {
            put(StructuredPostal.STREET, "Street")
            put(StructuredPostal.TYPE, StructuredPostal.TYPE_HOME)
        }, contact)
        assertArrayEquals(arrayOf(AddressType.HOME), contact.addresses[0].property.types.toTypedArray())
    }

    @Test
    fun testType_Other() {
        val contact = Contact()
        StructuredPostalHandler.handle(ContentValues().apply {
            put(StructuredPostal.STREET, "Street")
            put(StructuredPostal.TYPE, StructuredPostal.TYPE_OTHER)
        }, contact)
        assertTrue(contact.addresses[0].property.types.isEmpty())
    }

    @Test
    fun testType_Work() {
        val contact = Contact()
        StructuredPostalHandler.handle(ContentValues().apply {
            put(StructuredPostal.STREET, "Street")
            put(StructuredPostal.TYPE, StructuredPostal.TYPE_WORK)
        }, contact)
        assertArrayEquals(arrayOf(AddressType.WORK), contact.addresses[0].property.types.toTypedArray())
    }

}
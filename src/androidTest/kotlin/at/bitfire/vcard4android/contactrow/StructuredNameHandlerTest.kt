/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.content.ContentValues
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import at.bitfire.vcard4android.Contact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StructuredNameHandlerTest {

    @Test
    fun testEmpty() {
        val contact = Contact()
        StructuredNameHandler.handle(ContentValues().apply {
            putNull(StructuredName.FAMILY_NAME)
        }, contact)
        assertNull(contact.prefix)
        assertNull(contact.givenName)
        assertNull(contact.middleName)
        assertNull(contact.familyName)
        assertNull(contact.suffix)
        assertNull(contact.phoneticGivenName)
        assertNull(contact.phoneticMiddleName)
        assertNull(contact.phoneticFamilyName)
    }

    @Test
    fun testValues() {
        val contact = Contact()
        StructuredNameHandler.handle(ContentValues().apply {
            put(StructuredName.PREFIX, "P.")
            put(StructuredName.GIVEN_NAME, "Given")
            put(StructuredName.MIDDLE_NAME, "Middle")
            put(StructuredName.FAMILY_NAME, "Family")
            put(StructuredName.SUFFIX, "S")
            put(StructuredName.PHONETIC_GIVEN_NAME, "PhoneticGiven")
            put(StructuredName.PHONETIC_MIDDLE_NAME, "PhoneticMiddle")
            put(StructuredName.PHONETIC_FAMILY_NAME, "PhoneticFamily")
        }, contact)
        assertEquals("P.", contact.prefix)
        assertEquals("Given", contact.givenName)
        assertEquals("Middle", contact.middleName)
        assertEquals("Family", contact.familyName)
        assertEquals("S", contact.suffix)
        assertEquals("PhoneticGiven", contact.phoneticGivenName)
        assertEquals("PhoneticMiddle", contact.phoneticMiddleName)
        assertEquals("PhoneticFamily", contact.phoneticFamilyName)
    }

}
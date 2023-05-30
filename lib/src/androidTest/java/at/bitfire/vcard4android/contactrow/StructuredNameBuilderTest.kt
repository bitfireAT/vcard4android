/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import at.bitfire.vcard4android.Contact
import org.junit.Assert.assertEquals
import org.junit.Test

class StructuredNameBuilderTest {

    @Test
    fun testEmpty() {
        StructuredNameBuilder(Uri.EMPTY, null, Contact(), false).build().also { result ->
            assertEquals(0, result.size)
        }
    }
    
    @Test
    fun testValues() {
        StructuredNameBuilder(Uri.EMPTY, null, Contact().apply {
            prefix = "P."
            givenName = "Given"
            middleName = "Middle"
            familyName = "Family"
            suffix = "S"

            phoneticGivenName = "Phonetic Given"
            phoneticMiddleName = "Phonetic Middle"
            phoneticFamilyName = "Phonetic Family"
        }, false).build().also { result ->
            assertEquals(1, result.size)
            assertEquals(StructuredName.CONTENT_ITEM_TYPE, result[0].values[StructuredName.MIMETYPE])

            assertEquals("P.", result[0].values[StructuredName.PREFIX])
            assertEquals("Given", result[0].values[StructuredName.GIVEN_NAME])
            assertEquals("Middle", result[0].values[StructuredName.MIDDLE_NAME])
            assertEquals("Family", result[0].values[StructuredName.FAMILY_NAME])
            assertEquals("S", result[0].values[StructuredName.SUFFIX])

            assertEquals("Phonetic Given", result[0].values[StructuredName.PHONETIC_GIVEN_NAME])
            assertEquals("Phonetic Middle", result[0].values[StructuredName.PHONETIC_MIDDLE_NAME])
            assertEquals("Phonetic Family", result[0].values[StructuredName.PHONETIC_FAMILY_NAME])
        }
    }

}
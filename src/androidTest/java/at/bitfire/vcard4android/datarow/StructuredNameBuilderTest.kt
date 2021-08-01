package at.bitfire.vcard4android.datarow

import android.net.Uri
import android.provider.ContactsContract
import at.bitfire.vcard4android.Contact
import org.junit.Assert.assertEquals
import org.junit.Test

class StructuredNameBuilderTest {

    @Test
    fun testEmpty() {
        StructuredNameBuilder("", Uri.EMPTY, null, Contact()).build().also { result ->
            assertEquals(0, result.size)
        }
    }

    @Test
    fun testValues() {
        StructuredNameBuilder("", Uri.EMPTY, null, Contact().apply {
            prefix = "P."
            givenName = "Given"
            middleName = "Middle"
            familyName = "Family"
            suffix = "S"

            phoneticGivenName = "Phonetic Given"
            phoneticMiddleName = "Phonetic Middle"
            phoneticFamilyName = "Phonetic Family"
        }).build().also { result ->
            assertEquals(1, result.size)

            assertEquals("P.", result[0].values[ContactsContract.CommonDataKinds.StructuredName.PREFIX])
            assertEquals("Given", result[0].values[ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME])
            assertEquals("Middle", result[0].values[ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME])
            assertEquals("Family", result[0].values[ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME])
            assertEquals("S", result[0].values[ContactsContract.CommonDataKinds.StructuredName.SUFFIX])

            assertEquals("Phonetic Given", result[0].values[ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME])
            assertEquals("Phonetic Middle", result[0].values[ContactsContract.CommonDataKinds.StructuredName.PHONETIC_MIDDLE_NAME])
            assertEquals("Phonetic Family", result[0].values[ContactsContract.CommonDataKinds.StructuredName.PHONETIC_FAMILY_NAME])
        }
    }

}
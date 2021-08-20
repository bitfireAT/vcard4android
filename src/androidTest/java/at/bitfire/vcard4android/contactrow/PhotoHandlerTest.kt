package at.bitfire.vcard4android.contactrow

import android.content.ContentValues
import android.provider.ContactsContract.CommonDataKinds.Photo
import at.bitfire.vcard4android.Contact
import at.bitfire.vcard4android.datavalues.PhotoHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class PhotoHandlerTest {

    @Test
    fun testPhoto_Empty() {
        val contact = Contact()
        PhotoHandler(null).handle(ContentValues().apply {
            putNull(Photo.PHOTO)
        }, contact)
        assertNull(contact.photo)
    }

    @Test
    fun testPhoto_Blob() {
        val blob = ByteArray(1024) { Random.nextInt().toByte() }
        val contact = Contact()
        PhotoHandler(null).handle(ContentValues().apply {
            put(Photo.PHOTO, blob)
        }, contact)
        assertEquals(blob, contact.photo)
    }

    // TODO testPhoto_FileId

}
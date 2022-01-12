/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Photo
import androidx.test.platform.app.InstrumentationRegistry
import at.bitfire.vcard4android.Contact
import org.apache.commons.io.IOUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PhotoBuilderTest {

    @Test
    fun testEmpty() {
        PhotoBuilder(Uri.EMPTY, null, Contact()).build().also { result ->
            assertEquals(0, result.size)
        }
    }


    @Test
    fun testPhoto_NoResize() {
        val blob = ByteArray(1024) { Random.nextInt().toByte() }
        assertTrue(blob.size < PhotoBuilder.MAX_PHOTO_BLOB_SIZE)
        PhotoBuilder(Uri.EMPTY, null, Contact().apply {
            photo = blob
        }).build().also { result ->
            assertEquals(Photo.CONTENT_ITEM_TYPE, result[0].values[Photo.MIMETYPE])
            assertEquals(blob, result[0].values[Photo.PHOTO])
        }
    }

    @Test
    fun testPhoto_Resize() {
        val blob = IOUtils.readFully(InstrumentationRegistry.getInstrumentation().context.assets.open("large.jpg"), 3519652)
        assertTrue(blob.size > PhotoBuilder.MAX_PHOTO_BLOB_SIZE)
        PhotoBuilder(Uri.EMPTY, null, Contact().apply {
            photo = blob
        }).build().also { result ->
            assertEquals(Photo.CONTENT_ITEM_TYPE, result[0].values[Photo.MIMETYPE])
            assertTrue((result[0].values[Photo.PHOTO] as ByteArray).size < PhotoBuilder.MAX_PHOTO_BLOB_SIZE)
        }
    }

}
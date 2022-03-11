/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.net.Uri
import at.bitfire.vcard4android.Contact
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class PhotoBuilderTest {

    @Test
    fun testBuild_NoPhoto() {
        PhotoBuilder(Uri.EMPTY, null, Contact()).build().also { result ->
            assertEquals(0, result.size)
        }
    }

    @Test
    fun testBuild_Photo() {
        val blob = ByteArray(1024) { Random.nextInt().toByte() }
        PhotoBuilder(Uri.EMPTY, null, Contact().apply {
            photo = blob
        }).build().also { result ->
            // no row because photos have to be inserted with a separate call to insertPhoto()
            assertEquals(0, result.size)
        }
    }


    @Test
    fun testInsertPhoto() {
        // TODO
    }

    @Test
    fun testInsertPhoto_Invalid() {
        // TODO
    }

}
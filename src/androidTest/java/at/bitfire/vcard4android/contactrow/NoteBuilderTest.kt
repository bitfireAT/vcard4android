/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds
import at.bitfire.vcard4android.Contact
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteBuilderTest {

    @Test
    fun testNote_Empty() {
        NoteBuilder(Uri.EMPTY, null, Contact(), false).build().also { result ->
            assertEquals(0, result.size)
        }
    }

    @Test
    fun testNote_Blank() {
        NoteBuilder(Uri.EMPTY, null, Contact().apply {
            note = ""
        }, false).build().also { result ->
            assertEquals(0, result.size)
        }
    }

    @Test
    fun testNote_Value() {
        NoteBuilder(Uri.EMPTY, null, Contact().apply {
            note = "Some Note"
        }, false).build().also { result ->
            assertEquals(1, result.size)
            assertEquals(CommonDataKinds.Note.CONTENT_ITEM_TYPE, result[0].values[CommonDataKinds.Note.MIMETYPE])
            assertEquals("Some Note", result[0].values[CommonDataKinds.Note.NOTE])
        }
    }

}
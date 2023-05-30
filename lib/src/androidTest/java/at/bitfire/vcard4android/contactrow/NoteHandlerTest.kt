/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.content.ContentValues
import android.provider.ContactsContract.CommonDataKinds.Note
import at.bitfire.vcard4android.Contact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteHandlerTest {

    @Test
    fun testNote_Empty() {
        val contact = Contact()
        NoteHandler.handle(ContentValues().apply {
            putNull(Note.NOTE)
        }, contact)
        assertNull(contact.note)
    }

    @Test
    fun testNote_Value() {
        val contact = Contact()
        NoteHandler.handle(ContentValues().apply {
            put(Note.NOTE, "Some Note")
        }, contact)
        assertEquals("Some Note", contact.note)
    }

}
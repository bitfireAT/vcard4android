/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Note
import at.bitfire.vcard4android.BatchOperation
import at.bitfire.vcard4android.Contact

class NoteBuilder(dataRowUri: Uri, rawContactId: Long?, contact: Contact, readOnly: Boolean)
    : DataRowBuilder(Factory.mimeType(), dataRowUri, rawContactId, contact, readOnly) {

    override fun build(): List<BatchOperation.CpoBuilder> {
        val note = contact.note
        if (note.isNullOrBlank())
            return emptyList()

        return listOf(newDataRow()
                .withValue(Note.MIMETYPE, Note.CONTENT_ITEM_TYPE)
                .withValue(Note.NOTE, note))
    }


    object Factory: DataRowBuilder.Factory<NoteBuilder> {
        override fun mimeType() = Note.CONTENT_ITEM_TYPE
        override fun newInstance(dataRowUri: Uri, rawContactId: Long?, contact: Contact, readOnly: Boolean) =
            NoteBuilder(dataRowUri, rawContactId, contact, readOnly)
    }

}
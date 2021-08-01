package at.bitfire.vcard4android.datarow

import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Note
import at.bitfire.vcard4android.BatchOperation
import at.bitfire.vcard4android.Contact

class NoteBuilder(mimeType: String, dataRowUri: Uri, rawContactId: Long?, contact: Contact)
    : DataRowBuilder(mimeType, dataRowUri, rawContactId, contact) {

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
        override fun newInstance(dataRowUri: Uri, rawContactId: Long?, contact: Contact) =
            NoteBuilder(mimeType(), dataRowUri, rawContactId, contact)
    }

}
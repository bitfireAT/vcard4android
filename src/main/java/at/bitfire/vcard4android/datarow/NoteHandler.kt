package at.bitfire.vcard4android.datarow

import android.content.ContentValues
import android.provider.ContactsContract.CommonDataKinds.Note
import at.bitfire.vcard4android.Contact

object NoteHandler: DataRowHandler() {

    override fun forMimeType() = Note.CONTENT_ITEM_TYPE

    override fun handle(values: ContentValues, contact: Contact) {
        super.handle(values, contact)

        contact.note = values.getAsString(Note.NOTE)
    }

}
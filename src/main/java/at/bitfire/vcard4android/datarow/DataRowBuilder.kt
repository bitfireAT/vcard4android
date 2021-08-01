package at.bitfire.vcard4android.datarow

import android.net.Uri
import android.provider.ContactsContract
import at.bitfire.vcard4android.BatchOperation
import at.bitfire.vcard4android.Contact

/**
 * Builder for a data row to insert into the Android contact provider.
 *
 * @param dataRowUri    data row URI, including callerIsSyncAdapter=… if required
 * @param rawContactId  not *null*: raw contact ID to assign this data row to;
 *                      *null*: data row will be assigned to back reference with index 0
 *                      (= result ID of the first operation in batch, which must be the insert operation to insert the raw contact)
 */
abstract class DataRowBuilder(val mimeType: String, val dataRowUri: Uri, val rawContactId: Long?, val contact: Contact) {

    abstract fun build(): List<BatchOperation.CpoBuilder>


    protected fun newDataRow(): BatchOperation.CpoBuilder {
        val insert = BatchOperation.CpoBuilder
            .newInsert(dataRowUri)
            .withValue(ContactsContract.RawContacts.Data.MIMETYPE, mimeType)

        if (rawContactId != null)
            insert.withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
        else
            insert.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)

        return insert
    }


    interface Factory<T: DataRowBuilder> {

        fun mimeType(): String
        fun newInstance(dataRowUri: Uri, rawContactId: Long?, contact: Contact): T

    }

}
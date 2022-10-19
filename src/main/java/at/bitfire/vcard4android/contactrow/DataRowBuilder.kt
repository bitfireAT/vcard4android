/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.net.Uri
import android.provider.ContactsContract
import at.bitfire.vcard4android.BatchOperation
import at.bitfire.vcard4android.Contact

/**
 * Builder for a data row to insert into the Android contact provider.
 *
 * @param mimeType      the type of data in this row, see [ContactsContract.CommonDataKinds]
 * @param dataRowUri    data row URI, including callerIsSyncAdapter=… if required
 * @param rawContactId  not *null*: raw contact ID to assign this data row to;
 *                      *null*: data row will be assigned to back reference with index 0
 *                      (= result ID of the first operation in batch, which must be the insert operation to insert the raw contact)
 * @param contact       which contact this data row belongs to
 * @param readOnly      whether the data row is read-only (write protected). When the address book is
 *                      read-only, all contacts inside are too, and so the contact's data rows should be as well.
 */
abstract class DataRowBuilder(
    val mimeType: String,
    val dataRowUri: Uri,
    val rawContactId: Long?,
    val contact: Contact,
    val readOnly: Boolean
) {

    abstract fun build(): List<BatchOperation.CpoBuilder>


    protected fun newDataRow(): BatchOperation.CpoBuilder {
        val insert = BatchOperation.CpoBuilder
            .newInsert(dataRowUri)
            .withValue(ContactsContract.RawContacts.Data.MIMETYPE, mimeType)

        if (readOnly)
            insert.withValue(ContactsContract.Data.IS_READ_ONLY, 1)

        if (rawContactId != null)
            insert.withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
        else
            insert.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)

        return insert
    }


    interface Factory<T: DataRowBuilder> {
        fun mimeType(): String
        fun newInstance(dataRowUri: Uri, rawContactId: Long?, contact: Contact, readOnly: Boolean): T
    }

}
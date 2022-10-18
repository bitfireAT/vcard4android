/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import at.bitfire.vcard4android.BatchOperation
import at.bitfire.vcard4android.Contact

class StructuredNameBuilder(dataRowUri: Uri, rawContactId: Long?, contact: Contact, readOnly: Boolean)
    : DataRowBuilder(Factory.mimeType(), dataRowUri, rawContactId, contact, readOnly) {

    override fun build(): List<BatchOperation.CpoBuilder> {
        if (contact.displayName == null &&
            contact.prefix == null &&
            contact.givenName == null && contact.middleName == null && contact.familyName == null &&
            contact.suffix == null &&
            contact.phoneticGivenName == null && contact.phoneticMiddleName == null && contact.phoneticFamilyName == null)
                return emptyList()

        return listOf(newDataRow().apply {
            withValue(StructuredName.DISPLAY_NAME, contact.displayName)
            withValue(StructuredName.PREFIX, contact.prefix)
            withValue(StructuredName.GIVEN_NAME, contact.givenName)
            withValue(StructuredName.MIDDLE_NAME, contact.middleName)
            withValue(StructuredName.FAMILY_NAME, contact.familyName)
            withValue(StructuredName.SUFFIX, contact.suffix)
            withValue(StructuredName.PHONETIC_GIVEN_NAME, contact.phoneticGivenName)
            withValue(StructuredName.PHONETIC_MIDDLE_NAME, contact.phoneticMiddleName)
            withValue(StructuredName.PHONETIC_FAMILY_NAME, contact.phoneticFamilyName)
        })
    }


    object Factory: DataRowBuilder.Factory<StructuredNameBuilder> {
        override fun mimeType() = StructuredName.CONTENT_ITEM_TYPE
        override fun newInstance(dataRowUri: Uri, rawContactId: Long?, contact: Contact, readOnly: Boolean) =
            StructuredNameBuilder(dataRowUri, rawContactId, contact, readOnly)
    }

}
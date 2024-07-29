/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.content.ContentValues
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import at.bitfire.vcard4android.Contact

object StructuredNameHandler: DataRowHandler() {

    override fun forMimeType() = StructuredName.CONTENT_ITEM_TYPE

    override fun handle(values: ContentValues, contact: Contact) {
        super.handle(values, contact)

        contact.displayName = values.getAsString(StructuredName.DISPLAY_NAME)

        contact.prefix = values.getAsString(StructuredName.PREFIX)
        contact.givenName = values.getAsString(StructuredName.GIVEN_NAME)
        contact.middleName = values.getAsString(StructuredName.MIDDLE_NAME)
        contact.familyName = values.getAsString(StructuredName.FAMILY_NAME)
        contact.suffix = values.getAsString(StructuredName.SUFFIX)

        contact.phoneticGivenName = values.getAsString(StructuredName.PHONETIC_GIVEN_NAME)
        contact.phoneticMiddleName = values.getAsString(StructuredName.PHONETIC_MIDDLE_NAME)
        contact.phoneticFamilyName = values.getAsString(StructuredName.PHONETIC_FAMILY_NAME)
    }

}
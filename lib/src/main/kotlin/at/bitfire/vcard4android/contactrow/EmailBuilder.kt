/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import at.bitfire.vcard4android.BatchOperation
import at.bitfire.vcard4android.Constants
import at.bitfire.vcard4android.Contact
import at.bitfire.vcard4android.property.CustomType
import ezvcard.parameter.EmailType
import java.util.*
import java.util.logging.Level

class EmailBuilder(dataRowUri: Uri, rawContactId: Long?, contact: Contact, readOnly: Boolean)
    : DataRowBuilder(Factory.mimeType(), dataRowUri, rawContactId, contact, readOnly) {

    override fun build(): List<BatchOperation.CpoBuilder> {
        val result = LinkedList<BatchOperation.CpoBuilder>()
        for (labeledEmail in contact.emails) {
            if (labeledEmail.property.value.isNullOrBlank())
                continue

            val email = labeledEmail.property
            val types = email.types

            // preferred email address?
            var pref: Int? = null
            try {
                pref = email.pref
            } catch(e: IllegalStateException) {
                Constants.log.log(Level.FINER, "Can't understand email PREF", e)
            }
            var isPrimary = pref != null
            if (types.contains(EmailType.PREF)) {
                isPrimary = true
                types -= EmailType.PREF
            }

            var typeCode = Email.TYPE_OTHER
            var typeLabel: String? = null
            if (labeledEmail.label != null) {
                typeCode = Email.TYPE_CUSTOM
                typeLabel = labeledEmail.label
            } else {
                for (type in types)
                    when (type) {
                        EmailType.HOME -> typeCode = Email.TYPE_HOME
                        EmailType.WORK -> typeCode = Email.TYPE_WORK
                        CustomType.Email.MOBILE -> typeCode = Email.TYPE_MOBILE
                    }
            }

            result += newDataRow()
                    .withValue(Email.MIMETYPE, Email.CONTENT_ITEM_TYPE)
                    .withValue(Email.ADDRESS, email.value)
                    .withValue(Email.TYPE, typeCode)
                    .withValue(Email.LABEL, typeLabel)
                    .withValue(Email.IS_PRIMARY, if (isPrimary) 1 else 0)
                    .withValue(ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY, if (isPrimary) 1 else 0)
        }
        return result
    }


    object Factory: DataRowBuilder.Factory<EmailBuilder> {
        override fun mimeType() = Email.CONTENT_ITEM_TYPE
        override fun newInstance(dataRowUri: Uri, rawContactId: Long?, contact: Contact, readOnly: Boolean) =
            EmailBuilder(dataRowUri, rawContactId, contact, readOnly)
    }

}
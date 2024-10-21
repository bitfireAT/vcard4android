/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.content.ContentValues
import android.provider.ContactsContract.CommonDataKinds.Email
import at.bitfire.vcard4android.Contact
import at.bitfire.vcard4android.LabeledProperty
import at.bitfire.vcard4android.property.CustomType
import ezvcard.parameter.EmailType

object EmailHandler: DataRowHandler() {

    override fun forMimeType() = Email.CONTENT_ITEM_TYPE

    override fun handle(values: ContentValues, contact: Contact) {
        super.handle(values, contact)

        val address = values.getAsString(Email.ADDRESS) ?: return

        val email = ezvcard.property.Email(address)
        val labeledEmail = LabeledProperty(email)

        when (values.getAsInteger(Email.TYPE)) {
            Email.TYPE_HOME ->
                email.types += EmailType.HOME
            Email.TYPE_WORK ->
                email.types += EmailType.WORK
            Email.TYPE_MOBILE ->
                email.types += CustomType.Email.MOBILE
            Email.TYPE_CUSTOM ->
                values.getAsString(Email.LABEL)?.let {
                    labeledEmail.label = it
                }
        }
        if (values.getAsInteger(Email.IS_PRIMARY) != 0)
            email.pref = 1

        contact.emails += labeledEmail
   }

}
/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.content.ContentValues
import android.provider.ContactsContract.CommonDataKinds.SipAddress
import at.bitfire.vcard4android.Constants
import at.bitfire.vcard4android.Contact
import at.bitfire.vcard4android.LabeledProperty
import ezvcard.parameter.ImppType
import ezvcard.property.Impp

object SipAddressHandler: DataRowHandler() {

    override fun forMimeType() = SipAddress.CONTENT_ITEM_TYPE

    override fun handle(values: ContentValues, contact: Contact) {
        super.handle(values, contact)
        val sip = values.getAsString(SipAddress.SIP_ADDRESS) ?: return

        try {
            val impp = Impp("sip:$sip")
            val labeledImpp = LabeledProperty(impp)

            when (values.getAsInteger(SipAddress.TYPE)) {
                SipAddress.TYPE_HOME ->
                    impp.types += ImppType.HOME
                SipAddress.TYPE_WORK ->
                    impp.types += ImppType.WORK
                SipAddress.TYPE_CUSTOM ->
                    values.getAsString(SipAddress.LABEL)?.let {
                        labeledImpp.label = it
                    }
            }
            contact.impps.add(labeledImpp)
        } catch(e: IllegalArgumentException) {
            Constants.log.warning("Ignoring invalid locally stored SIP address")
        }
   }

}
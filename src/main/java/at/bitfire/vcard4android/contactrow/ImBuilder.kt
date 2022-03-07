/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Im
import at.bitfire.vcard4android.BatchOperation
import at.bitfire.vcard4android.Constants
import at.bitfire.vcard4android.Contact
import ezvcard.parameter.ImppType
import java.util.*

class ImBuilder(dataRowUri: Uri, rawContactId: Long?, contact: Contact)
    : DataRowBuilder(Factory.mimeType(), dataRowUri, rawContactId, contact) {

    @Suppress("DEPRECATION")
    override fun build(): List<BatchOperation.CpoBuilder> {
        val result = LinkedList<BatchOperation.CpoBuilder>()
        for (labeledIm in contact.impps) {
            val impp = labeledIm.property

            val protocol = impp.protocol
            if (protocol == null) {
                Constants.log.warning("Ignoring IM address without protocol")
                continue
            }

            var typeCode: Int = Im.TYPE_OTHER
            var typeLabel: String? = null
            if (labeledIm.label != null) {
                typeCode = Im.TYPE_CUSTOM
                typeLabel = labeledIm.label
            } else {
                for (type in impp.types)
                    when (type) {
                        ImppType.HOME,
                        ImppType.PERSONAL -> typeCode = Im.TYPE_HOME
                        ImppType.WORK,
                        ImppType.BUSINESS -> typeCode = Im.TYPE_WORK
                    }
            }

            var protocolCode: Int
            var protocolLabel: String? = null

            when {
                impp.isAim -> protocolCode = Im.PROTOCOL_AIM
                impp.isMsn -> protocolCode = Im.PROTOCOL_MSN
                impp.isYahoo -> protocolCode = Im.PROTOCOL_YAHOO
                impp.isSkype -> protocolCode = Im.PROTOCOL_SKYPE
                impp.isIcq -> protocolCode = Im.PROTOCOL_ICQ
                impp.isXmpp || protocol.equals("jabber", true) -> protocolCode = Im.PROTOCOL_JABBER
                protocol.equals("qq", true) -> protocolCode = Im.PROTOCOL_QQ
                protocol.equals("google-talk", true) -> protocolCode = Im.PROTOCOL_GOOGLE_TALK
                protocol.equals("netmeeting", true) -> protocolCode = Im.PROTOCOL_NETMEETING
                protocol.equals("sip", true) -> continue // IMPP:sip:…  is handled by SipAddressBuilder
                else -> {
                    protocolCode = Im.PROTOCOL_CUSTOM
                    protocolLabel = protocol
                }
            }

            // save as IM address
            result += newDataRow()
                .withValue(Im.DATA, impp.handle)
                .withValue(Im.TYPE, typeCode)
                .withValue(Im.LABEL, typeLabel)
                .withValue(Im.PROTOCOL, protocolCode)
                .withValue(Im.CUSTOM_PROTOCOL, protocolLabel)
        }
        return result
    }


    object Factory: DataRowBuilder.Factory<ImBuilder> {
        override fun mimeType() = Im.CONTENT_ITEM_TYPE
        override fun newInstance(dataRowUri: Uri, rawContactId: Long?, contact: Contact) =
            ImBuilder(dataRowUri, rawContactId, contact)
    }

}
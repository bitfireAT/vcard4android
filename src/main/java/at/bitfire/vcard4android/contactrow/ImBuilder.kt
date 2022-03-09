/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Im
import at.bitfire.vcard4android.BatchOperation
import at.bitfire.vcard4android.Contact
import at.bitfire.vcard4android.property.CustomType
import ezvcard.parameter.ImppType
import java.util.*

class ImBuilder(dataRowUri: Uri, rawContactId: Long?, contact: Contact)
    : DataRowBuilder(Factory.mimeType(), dataRowUri, rawContactId, contact) {

    override fun build(): List<BatchOperation.CpoBuilder> {
        val result = LinkedList<BatchOperation.CpoBuilder>()
        for (labeledIm in contact.impps) {
            val impp = labeledIm.property

            if ((impp.uri.scheme == null && impp.uri.schemeSpecificPart == "") ||   // empty URI
                ImMapping.SCHEME_SIP.equals(impp.uri.scheme, true))       // handled by SipAddressBuilder
                continue

            var typeCode = Im.TYPE_OTHER
            var typeLabel: String? = null
            if (labeledIm.label != null) {
                typeCode = ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM
                typeLabel = labeledIm.label
            } else
                for (type in impp.types)
                    when (type) {
                        ImppType.HOME,
                        ImppType.PERSONAL -> typeCode = Im.TYPE_HOME
                        ImppType.WORK,
                        ImppType.BUSINESS -> typeCode = Im.TYPE_WORK
                    }

            var protocolCode = Im.PROTOCOL_CUSTOM

            // We parse SERVICE-TYPE (for instance used by iCloud), but don't use it actively.
            val serviceType = impp.getParameter(CustomType.Im.PARAMETER_SERVICE_TYPE)
                ?: impp.getParameter(CustomType.Im.PARAMETER_SERVICE_TYPE_ALT)
                ?: impp.getParameter("TYPE")

            // look for known messengers
            val customProtocol: String?
            val user: String
            ImMapping.uriToMessenger(impp.uri, serviceType).let { (messenger, handle) ->
                customProtocol = messenger
                user = handle
            }

            if (Build.VERSION.SDK_INT < 31) {
                /* On Android <12, we assign specific protocols like AIM etc. although most of them are not used anymore.
                   It's impossible to keep an up-to-date table of messengers, which is probably the reason why these
                   constants were deprecated in Android 12 (SDK level 31). */
                @Suppress("DEPRECATION")
                when (customProtocol) {
                    ImMapping.MESSENGER_AIM -> protocolCode = Im.PROTOCOL_AIM
                    ImMapping.MESSENGER_ICQ -> protocolCode = Im.PROTOCOL_ICQ
                    ImMapping.MESSENGER_SKYPE -> protocolCode = Im.PROTOCOL_SKYPE
                    ImMapping.MESSENGER_QQ -> protocolCode = Im.PROTOCOL_QQ
                    ImMapping.MESSENGER_XMPP -> protocolCode = Im.PROTOCOL_JABBER
                }
            }

            // save as IM address
            result += newDataRow()
                .withValue(Im.DATA, user)
                .withValue(Im.TYPE, typeCode)
                .withValue(Im.LABEL, typeLabel)
                .withValue(Im.PROTOCOL, protocolCode)
                .withValue(Im.CUSTOM_PROTOCOL, customProtocol)
        }
        return result
    }


    object Factory: DataRowBuilder.Factory<ImBuilder> {
        override fun mimeType() = Im.CONTENT_ITEM_TYPE
        override fun newInstance(dataRowUri: Uri, rawContactId: Long?, contact: Contact) =
            ImBuilder(dataRowUri, rawContactId, contact)
    }

}
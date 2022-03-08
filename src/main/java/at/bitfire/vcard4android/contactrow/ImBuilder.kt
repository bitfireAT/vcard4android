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
import org.apache.commons.lang3.StringUtils
import java.util.*

class ImBuilder(dataRowUri: Uri, rawContactId: Long?, contact: Contact)
    : DataRowBuilder(Factory.mimeType(), dataRowUri, rawContactId, contact) {

    override fun build(): List<BatchOperation.CpoBuilder> {
        val result = LinkedList<BatchOperation.CpoBuilder>()
        for (labeledIm in contact.impps) {
            val impp = labeledIm.property

            val protocol = impp.protocol ?: ""
            /*if (protocol == null) {
                Constants.log.warning("Ignoring IM address without protocol")
                continue
            }*/

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
            var customProtocol: String? = null

            if (Build.VERSION.SDK_INT >= 31) {
                // Since API level 31, PROTOCOL_XXX values are deprecated and only PROTOCOL_CUSTOM should be used.

            } else {
                /* On Android <12, we assign specific protocols like AIM etc. although most of them are not used anymore.
                   It's impossible to keep an up-to-date table of messengers, which is probably the reason why these
                   constants were deprecated. */
                @Suppress("DEPRECATION")
                when {
                    protocol.equals(CustomType.Im.PROTOCOL_AIM, true) ->
                        protocolCode = Im.PROTOCOL_AIM
                    protocol.equals(CustomType.Im.PROTOCOL_GOOGLE_TALK, true) ||
                            protocol.equals(CustomType.Im.PROTOCOL_GOOGLE_TALK_ALT, true) ->
                        protocolCode = Im.PROTOCOL_GOOGLE_TALK
                    protocol.equals(CustomType.Im.PROTOCOL_ICQ, true) ->
                        protocolCode = Im.PROTOCOL_ICQ
                    protocol.equals(CustomType.Im.PROTOCOL_XMPP, true) ->
                        protocolCode = Im.PROTOCOL_JABBER
                    protocol.equals(CustomType.Im.PROTOCOL_MSN, true) ||
                    protocol.equals(CustomType.Im.PROTOCOL_MSN_ALT, true) ->
                        protocolCode = Im.PROTOCOL_MSN
                    protocol.equals(CustomType.Im.PROTOCOL_QQ, true) ||
                            protocol.equals(CustomType.Im.PROTOCOL_QQ_ALT, true) ->
                        protocolCode = Im.PROTOCOL_QQ
                    protocol.equals(CustomType.Im.PROTOCOL_CALLTO, true) ||     // includes NetMeeting, which is dead
                    protocol.equals(CustomType.Im.PROTOCOL_SKYPE, true) ->
                        protocolCode = Im.PROTOCOL_SKYPE
                    protocol.equals(CustomType.Im.PROTOCOL_YAHOO, true) ->
                        protocolCode = Im.PROTOCOL_YAHOO

                    protocol.equals(CustomType.Im.PROTOCOL_SIP, true) ->
                        // IMPP:sip:…  is handled by SipAddressBuilder
                        continue
                }
            }

            if (protocolCode == Im.PROTOCOL_CUSTOM) {
                // We parse SERVICE-TYPE (for instance used by iCloud), but don't use it actively.
                val serviceType =
                    impp.getParameter(CustomType.Im.PARAMETER_SERVICE_TYPE) ?:
                    impp.getParameter(CustomType.Im.PARAMETER_SERVICE_TYPE_ALT)

                customProtocol =                                        // protocol name shown in Android
                    serviceType?.let { StringUtils.capitalize(it) } ?:  // use service type, if available
                    StringUtils.capitalize(protocol)                    // fall back to raw URI scheme
            }

            // save as IM address
            result += newDataRow()
                .withValue(Im.DATA, impp.handle)
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
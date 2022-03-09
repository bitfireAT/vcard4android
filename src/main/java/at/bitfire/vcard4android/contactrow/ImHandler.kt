/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.content.ContentValues
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Im
import at.bitfire.vcard4android.Constants
import at.bitfire.vcard4android.Contact
import at.bitfire.vcard4android.LabeledProperty
import at.bitfire.vcard4android.Utils.normalizeNFD
import at.bitfire.vcard4android.property.CustomType
import ezvcard.parameter.ImppType
import ezvcard.property.Impp
import org.apache.commons.lang3.StringUtils
import java.net.URI
import java.util.logging.Level

object ImHandler: DataRowHandler() {

    override fun forMimeType() = Im.CONTENT_ITEM_TYPE

    @Suppress("DEPRECATION")
    override fun handle(values: ContentValues, contact: Contact) {
        super.handle(values, contact)

        val handle = values.getAsString(Im.DATA)
        if (handle == null) {
            Constants.log.warning("Ignoring IM without handle")
            return
        }

        val protocolCode = values.getAsInteger(Im.PROTOCOL)
        val impp = when (protocolCode) {
            Im.PROTOCOL_AIM ->
                Impp.aim(handle)
            Im.PROTOCOL_MSN ->
                Impp.msn(handle)
            Im.PROTOCOL_SKYPE ->
                Impp.skype(handle)
            Im.PROTOCOL_GOOGLE_TALK ->
                Impp(CustomType.Im.PROTOCOL_GOOGLE_TALK, handle)
            Im.PROTOCOL_ICQ ->
                Impp.icq(handle)
            Im.PROTOCOL_JABBER ->
                Impp.xmpp(handle)
            Im.PROTOCOL_NETMEETING ->
                Impp.skype(handle)      // NetMeeting is dead and has most likely been replaced by Skype
            Im.PROTOCOL_QQ ->
                Impp(CustomType.Im.PROTOCOL_QQ, handle)
            Im.PROTOCOL_YAHOO ->
                Impp.yahoo(handle)
            Im.PROTOCOL_CUSTOM -> {
                val customProtocol = values.getAsString(Im.CUSTOM_PROTOCOL)
                Impp(ImMapping.messengerToUri(customProtocol, handle))
            }
            else -> {
                Constants.log.log(Level.WARNING, "Unknown IM protocol: $protocolCode")
                return
            }
        }

        val labeledImpp = LabeledProperty(impp)
        when (values.getAsInteger(Im.TYPE)) {
            Im.TYPE_HOME ->
                impp.types += ImppType.HOME
            Im.TYPE_WORK ->
                impp.types += ImppType.WORK
            Im.TYPE_CUSTOM ->
                values.getAsString(ContactsContract.CommonDataKinds.Email.LABEL)?.let {
                    labeledImpp.label = it
                }
        }

        contact.impps += labeledImpp
    }

}
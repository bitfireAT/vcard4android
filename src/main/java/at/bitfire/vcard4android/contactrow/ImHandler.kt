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
import at.bitfire.vcard4android.property.CustomType
import ezvcard.parameter.ImppType
import ezvcard.property.Impp
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
        val messenger = when (protocolCode) {
            Im.PROTOCOL_AIM -> ImMapping.MESSENGER_AIM
            Im.PROTOCOL_MSN,
            Im.PROTOCOL_SKYPE -> ImMapping.MESSENGER_SKYPE
            Im.PROTOCOL_GOOGLE_TALK -> "GoogleTalk"     // dead
            Im.PROTOCOL_ICQ -> ImMapping.MESSENGER_ICQ
            Im.PROTOCOL_JABBER -> ImMapping.MESSENGER_XMPP
            Im.PROTOCOL_NETMEETING -> "NetMeeting"      // dead
            Im.PROTOCOL_QQ -> ImMapping.MESSENGER_QQ
            Im.PROTOCOL_YAHOO -> "Yahoo"                // dead
            Im.PROTOCOL_CUSTOM ->
                values.getAsString(Im.CUSTOM_PROTOCOL)
            else -> {
                Constants.log.log(Level.WARNING, "Unknown IM protocol: $protocolCode")
                return
            }
        }
        val impp = Impp(ImMapping.messengerToUri(messenger, handle))

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
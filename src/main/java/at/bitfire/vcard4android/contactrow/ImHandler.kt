/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.content.ContentValues
import android.provider.ContactsContract.CommonDataKinds.Im
import at.bitfire.vcard4android.Constants
import at.bitfire.vcard4android.Contact
import at.bitfire.vcard4android.LabeledProperty
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
            Constants.log.warning("Ignoring instant messenger record without handle")
            return
        }

        val labeledImpp = when (values.getAsInteger(Im.PROTOCOL)) {
            Im.PROTOCOL_AIM ->
                LabeledProperty(Impp.aim(handle))
            Im.PROTOCOL_MSN ->
                LabeledProperty(Impp.msn(handle))
            Im.PROTOCOL_YAHOO ->
                LabeledProperty(Impp.yahoo(handle))
            Im.PROTOCOL_SKYPE ->
                LabeledProperty(Impp.skype(handle))
            Im.PROTOCOL_QQ ->
                LabeledProperty(Impp("qq", handle))
            Im.PROTOCOL_GOOGLE_TALK ->
                LabeledProperty(Impp("google-talk", handle))
            Im.PROTOCOL_ICQ ->
                LabeledProperty(Impp.icq(handle))
            Im.PROTOCOL_JABBER ->
                LabeledProperty(Impp.xmpp(handle))
            Im.PROTOCOL_NETMEETING ->
                LabeledProperty(Impp("netmeeting", handle))
            Im.PROTOCOL_CUSTOM ->
                try {
                    LabeledProperty(
                        Impp(protocolToUriScheme(values.getAsString(Im.CUSTOM_PROTOCOL)), handle),
                        values.getAsString(Im.CUSTOM_PROTOCOL)
                    )
                } catch(e: IllegalArgumentException) {
                    Constants.log.warning("Messenger type/value can't be expressed as URI; ignoring")
                    return
                }
            else -> {
                Constants.log.log(Level.WARNING, "Unknown IM type", values)
                return
            }
        }
        val impp = labeledImpp.property

        when (values.getAsInteger(Im.TYPE)) {
            Im.TYPE_HOME ->
                impp.types += ImppType.HOME
            Im.TYPE_WORK ->
                impp.types += ImppType.WORK
        }

        contact.impps += labeledImpp
    }

    fun protocolToUriScheme(s: String?) =
            // RFC 3986 3.1
            // scheme      = ALPHA *( ALPHA / DIGIT / "+" / "-" / "." )
            // ALPHA       =  %x41-5A / %x61-7A   ; A-Z / a-z
                // DIGIT       =  %x30-39             ; 0-9
            s?.replace(Regex("^[^a-zA-Z]+"), "")?.replace(Regex("[^\\da-zA-Z+-.]"), "")?.lowercase()

}
/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.content.ContentValues
import android.provider.ContactsContract.CommonDataKinds.Nickname
import at.bitfire.vcard4android.Contact
import at.bitfire.vcard4android.LabeledProperty
import at.bitfire.vcard4android.property.CustomType

object NicknameHandler: DataRowHandler() {

    override fun forMimeType() = Nickname.CONTENT_ITEM_TYPE

    override fun handle(values: ContentValues, contact: Contact) {
        super.handle(values, contact)

        val name = values.getAsString(Nickname.NAME) ?: return
        val nick = ezvcard.property.Nickname()
        val labeledNick = LabeledProperty(nick)

        nick.values += name

        when (values.getAsInteger(Nickname.TYPE)) {
            Nickname.TYPE_MAIDEN_NAME ->
                nick.type = CustomType.Nickname.MAIDEN_NAME
            Nickname.TYPE_SHORT_NAME ->
                nick.type = CustomType.Nickname.SHORT_NAME
            Nickname.TYPE_INITIALS ->
                nick.type = CustomType.Nickname.INITIALS
            Nickname.TYPE_CUSTOM ->
                values.getAsString(Nickname.LABEL)?.let { labeledNick.label = it }
        }

        contact.nickName = labeledNick
    }

}
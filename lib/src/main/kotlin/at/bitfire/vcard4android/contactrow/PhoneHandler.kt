/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.content.ContentValues
import android.provider.ContactsContract.CommonDataKinds.Phone
import at.bitfire.vcard4android.Contact
import at.bitfire.vcard4android.LabeledProperty
import at.bitfire.vcard4android.property.CustomType
import ezvcard.parameter.TelephoneType
import ezvcard.property.Telephone

object PhoneHandler: DataRowHandler() {

    override fun forMimeType() = Phone.CONTENT_ITEM_TYPE

    override fun handle(values: ContentValues, contact: Contact) {
        super.handle(values, contact)

        val number = values.getAsString(Phone.NUMBER) ?: return
        val tel = Telephone(number)
        val labeledNumber = LabeledProperty(tel)

        when (values.getAsInteger(Phone.TYPE)) {
            Phone.TYPE_HOME ->
                tel.types += TelephoneType.HOME
            Phone.TYPE_MOBILE ->
                tel.types += TelephoneType.CELL
            Phone.TYPE_WORK ->
                tel.types += TelephoneType.WORK
            Phone.TYPE_FAX_WORK -> {
                tel.types += TelephoneType.FAX
                tel.types += TelephoneType.WORK
            }
            Phone.TYPE_FAX_HOME -> {
                tel.types += TelephoneType.FAX
                tel.types += TelephoneType.HOME
            }
            Phone.TYPE_PAGER ->
                tel.types += TelephoneType.PAGER
            Phone.TYPE_CALLBACK ->
                tel.types += CustomType.Phone.CALLBACK
            Phone.TYPE_CAR ->
                tel.types += TelephoneType.CAR
            Phone.TYPE_COMPANY_MAIN ->
                tel.types += CustomType.Phone.COMPANY_MAIN
            Phone.TYPE_ISDN ->
                tel.types += TelephoneType.ISDN
            Phone.TYPE_MAIN ->
                tel.types += TelephoneType.VOICE
            Phone.TYPE_OTHER_FAX ->
                tel.types += TelephoneType.FAX
            Phone.TYPE_RADIO ->
                tel.types += CustomType.Phone.RADIO
            Phone.TYPE_TELEX ->
                tel.types += TelephoneType.TEXTPHONE
            Phone.TYPE_TTY_TDD ->
                tel.types += TelephoneType.TEXT
            Phone.TYPE_WORK_MOBILE -> {
                tel.types += TelephoneType.CELL
                tel.types += TelephoneType.WORK
            }
            Phone.TYPE_WORK_PAGER -> {
                tel.types += TelephoneType.PAGER
                tel.types += TelephoneType.WORK
            }
            Phone.TYPE_ASSISTANT ->
                tel.types += CustomType.Phone.ASSISTANT
            Phone.TYPE_MMS ->
                tel.types += CustomType.Phone.MMS
            Phone.TYPE_CUSTOM -> {
                values.getAsString(Phone.LABEL)?.let { label ->
                    labeledNumber.label = label
                }
            }
        }
        if (values.getAsInteger(Phone.IS_PRIMARY) != 0)
            tel.pref = 1

        contact.phoneNumbers += labeledNumber
    }

}
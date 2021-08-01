package at.bitfire.vcard4android.datarow

import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import at.bitfire.vcard4android.BatchOperation
import at.bitfire.vcard4android.Constants
import at.bitfire.vcard4android.Contact
import ezvcard.parameter.TelephoneType
import java.util.*
import java.util.logging.Level

class PhoneBuilder(mimeType: String, dataRowUri: Uri, rawContactId: Long?, contact: Contact)
    : DataRowBuilder(mimeType, dataRowUri, rawContactId, contact) {

    override fun build(): List<BatchOperation.CpoBuilder> {
        val result = LinkedList<BatchOperation.CpoBuilder>()
        for (phoneNumber in contact.phoneNumbers) {
            val number = phoneNumber.property
            if (number.text.isNullOrBlank())
                continue

            val types = number.types

            // preferred number?
            var pref: Int? = null
            try {
                pref = number.pref
            } catch(e: IllegalStateException) {
                Constants.log.log(Level.FINER, "Can't understand phone number PREF", e)
            }
            var isPrimary = pref != null
            if (types.contains(TelephoneType.PREF)) {
                isPrimary = true
                types -= TelephoneType.PREF
            }

            var typeCode: Int = Phone.TYPE_OTHER
            var typeLabel: String? = null
            if (phoneNumber.label != null) {
                typeCode = Phone.TYPE_CUSTOM
                typeLabel = phoneNumber.label
            } else {
                when {
                    // 1 Android type <-> 2 vCard types: fax, cell, pager
                    types.contains(TelephoneType.FAX) ->
                        typeCode = when {
                            types.contains(TelephoneType.HOME) -> Phone.TYPE_FAX_HOME
                            types.contains(TelephoneType.WORK) -> Phone.TYPE_FAX_WORK
                            else                               -> Phone.TYPE_OTHER_FAX
                        }
                    types.contains(TelephoneType.CELL) ->
                        typeCode = if (types.contains(TelephoneType.WORK))
                            Phone.TYPE_WORK_MOBILE
                        else
                            Phone.TYPE_MOBILE
                    types.contains(TelephoneType.PAGER) ->
                        typeCode = if (types.contains(TelephoneType.WORK))
                            Phone.TYPE_WORK_PAGER
                        else
                            Phone.TYPE_PAGER

                    // types with 1:1 translation
                    types.contains(TelephoneType.HOME) ->
                        typeCode = Phone.TYPE_HOME
                    types.contains(TelephoneType.WORK) ->
                        typeCode = Phone.TYPE_WORK
                    types.contains(Contact.PHONE_TYPE_CALLBACK) ->
                        typeCode = Phone.TYPE_CALLBACK
                    types.contains(TelephoneType.CAR) ->
                        typeCode = Phone.TYPE_CAR
                    types.contains(Contact.PHONE_TYPE_COMPANY_MAIN) ->
                        typeCode = Phone.TYPE_COMPANY_MAIN
                    types.contains(TelephoneType.ISDN) ->
                        typeCode = Phone.TYPE_ISDN
                    types.contains(Contact.PHONE_TYPE_RADIO) ->
                        typeCode = Phone.TYPE_RADIO
                    types.contains(Contact.PHONE_TYPE_ASSISTANT) ->
                        typeCode = Phone.TYPE_ASSISTANT
                    types.contains(Contact.PHONE_TYPE_MMS) ->
                        typeCode = Phone.TYPE_MMS
                }
            }

            result += newDataRow()
                .withValue(Phone.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                .withValue(Phone.NUMBER, number.text)
                .withValue(Phone.TYPE, typeCode)
                .withValue(Phone.LABEL, typeLabel)
                .withValue(Phone.IS_PRIMARY, if (isPrimary) 1 else 0)
                .withValue(Phone.IS_SUPER_PRIMARY, if (isPrimary) 1 else 0)
        }
        return result
    }


    object Factory: DataRowBuilder.Factory<PhoneBuilder> {
        override fun mimeType() = Phone.CONTENT_ITEM_TYPE
        override fun newInstance(dataRowUri: Uri, rawContactId: Long?, contact: Contact) =
            PhoneBuilder(mimeType(), dataRowUri, rawContactId, contact)
    }

}
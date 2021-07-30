package at.bitfire.vcard4android.property

import ezvcard.io.scribe.StringPropertyScribe
import ezvcard.property.Kind

class XAddressBookServerKind(value: String?): Kind(value) {

    object Scribe :
        StringPropertyScribe<XAddressBookServerKind>(XAddressBookServerKind::class.java, "X-ADDRESSBOOKSERVER-KIND") {

        override fun _parseValue(value: String?) = XAddressBookServerKind(value)

    }

}
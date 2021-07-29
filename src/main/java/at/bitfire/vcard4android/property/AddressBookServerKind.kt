package at.bitfire.vcard4android.property

import ezvcard.io.scribe.KindScribe
import ezvcard.io.scribe.StringPropertyScribe
import ezvcard.io.scribe.UriPropertyScribe
import ezvcard.property.Kind
import ezvcard.property.TextProperty
import ezvcard.property.UriProperty

class AddressBookServerKind(value: String?): Kind(value) {

    object Scribe :
        StringPropertyScribe<AddressBookServerKind>(AddressBookServerKind::class.java, "X-ADDRESSBOOKSERVER-KIND") {

        override fun _parseValue(value: String?) = AddressBookServerKind(value)

    }

}
package at.bitfire.vcard4android.property

import ezvcard.io.scribe.StringPropertyScribe
import ezvcard.property.TextProperty

class PhoneticLastName(value: String?): TextProperty(value) {

    object Scribe :
        StringPropertyScribe<PhoneticLastName>(PhoneticLastName::class.java, "X-PHONETIC-LAST-NAME") {

        override fun _parseValue(value: String?) = PhoneticLastName(value)

    }

}
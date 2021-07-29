package at.bitfire.vcard4android.property

import ezvcard.io.scribe.StringPropertyScribe
import ezvcard.property.TextProperty

class PhoneticFirstName(value: String?): TextProperty(value) {

    object Scribe :
        StringPropertyScribe<PhoneticFirstName>(PhoneticFirstName::class.java, "X-PHONETIC-FIRST-NAME") {

        override fun _parseValue(value: String?) = PhoneticFirstName(value)

    }

}
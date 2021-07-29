package at.bitfire.vcard4android.property

import ezvcard.io.scribe.StringPropertyScribe
import ezvcard.property.TextProperty

class PhoneticMiddleName(value: String?): TextProperty(value) {

    object Scribe :
        StringPropertyScribe<PhoneticMiddleName>(PhoneticMiddleName::class.java, "X-PHONETIC-MIDDLE-NAME") {

        override fun _parseValue(value: String?) = PhoneticMiddleName(value)

    }

}
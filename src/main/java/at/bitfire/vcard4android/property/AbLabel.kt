package at.bitfire.vcard4android.property

import ezvcard.io.scribe.StringPropertyScribe
import ezvcard.property.TextProperty

class AbLabel(value: String?): TextProperty(value) {

    object Scribe :
        StringPropertyScribe<AbLabel>(AbLabel::class.java, "X-ABLabel") {

        override fun _parseValue(value: String?) = AbLabel(value)

    }

}
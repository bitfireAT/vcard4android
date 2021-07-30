package at.bitfire.vcard4android.property

import ezvcard.io.scribe.StringPropertyScribe
import ezvcard.property.TextProperty

class XAbLabel(value: String?): TextProperty(value) {

    object Scribe :
        StringPropertyScribe<XAbLabel>(XAbLabel::class.java, "X-ABLABEL") {

        override fun _parseValue(value: String?) = XAbLabel(value)

    }

}
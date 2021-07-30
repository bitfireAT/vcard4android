package at.bitfire.vcard4android.property

import android.net.Uri
import ezvcard.io.scribe.ImppScribe
import ezvcard.io.scribe.StringPropertyScribe
import ezvcard.io.scribe.UriPropertyScribe
import ezvcard.property.Impp
import ezvcard.property.TextProperty
import ezvcard.property.UriProperty

class XSip(value: String?): TextProperty(value) {

    object Scribe : StringPropertyScribe<XSip>(XSip::class.java, "X-SIP") {

        override fun _parseValue(value: String?) = XSip(value)

    }

}
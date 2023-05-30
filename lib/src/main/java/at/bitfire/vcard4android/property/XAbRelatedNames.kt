/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.property

import ezvcard.io.scribe.StringPropertyScribe
import ezvcard.property.TextProperty

class XAbRelatedNames(value: String?): TextProperty(value) {

    companion object {

        const val APPLE_ASSISTANT = "_\$!<Assistant>!\$_"
        const val APPLE_BROTHER = "_\$!<Brother>!\$_"
        const val APPLE_CHILD = "_\$!<Child>!\$_"
        const val APPLE_FATHER = "_\$!<Father>!\$_"
        const val APPLE_FRIEND = "_\$!<Friend>!\$_"
        const val APPLE_MANAGER = "_\$!<Manager>!\$_"
        const val APPLE_MOTHER = "_\$!<Mother>!\$_"
        const val APPLE_PARENT = "_\$!<Parent>!\$_"
        const val APPLE_PARTNER = "_\$!<Partner>!\$_"
        const val APPLE_SISTER = "_\$!<Sister>!\$_"
        const val APPLE_SPOUSE = "_\$!<Spouse>!\$_"

    }

    object Scribe :
        StringPropertyScribe<XAbRelatedNames>(XAbRelatedNames::class.java, "X-ABRELATEDNAMES") {

        override fun _parseValue(value: String?) = XAbRelatedNames(value)

    }

}
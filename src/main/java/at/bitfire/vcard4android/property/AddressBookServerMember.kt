package at.bitfire.vcard4android.property

import ezvcard.io.scribe.StringPropertyScribe
import ezvcard.io.scribe.UriPropertyScribe
import ezvcard.property.Member
import ezvcard.property.TextProperty
import ezvcard.property.UriProperty

class AddressBookServerMember(value: String?): Member(value) {

    object Scribe :
        UriPropertyScribe<AddressBookServerMember>(AddressBookServerMember::class.java, "X-ADDRESSBOOKSERVER-MEMBER") {

        override fun _parseValue(value: String?) = AddressBookServerMember(value)

    }

}
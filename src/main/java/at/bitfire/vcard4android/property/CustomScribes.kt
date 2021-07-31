package at.bitfire.vcard4android.property

import ezvcard.io.chain.ChainingTextWriter
import ezvcard.io.scribe.ScribeIndex
import ezvcard.io.text.VCardReader
import ezvcard.io.text.VCardWriter

object CustomScribes {

    /** list of all custom scribes (will be registered to readers/writers) **/
    val customScribes = arrayOf(
        XAbDate.Scribe,
        XAbLabel.Scribe,
        XAbRelatedNames.Scribe,
        XAddressBookServerKind.Scribe,
        XAddressBookServerMember.Scribe,
        XPhoneticFirstName.Scribe,
        XPhoneticMiddleName.Scribe,
        XPhoneticLastName.Scribe,
        XSip.Scribe
    )


    fun ChainingTextWriter.registerCustomScribes(): ChainingTextWriter {
        for (scribe in customScribes)
            register(scribe)
        return this
    }

    fun VCardReader.registerCustomScribes(): VCardReader {
        for (scribe in customScribes)
            scribeIndex.register(scribe)
        return this
    }

    fun VCardWriter.registerCustomScribes() {
        for (scribe in customScribes)
            scribeIndex.register(scribe)
    }

}

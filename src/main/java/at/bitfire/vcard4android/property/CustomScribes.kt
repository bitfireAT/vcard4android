package at.bitfire.vcard4android.property

import ezvcard.io.chain.ChainingTextWriter
import ezvcard.io.scribe.ScribeIndex
import ezvcard.io.text.VCardReader

object CustomScribes {

    /** list of all custom scribes (will be registered to readers/writers) **/
    val customScribes = arrayOf(
        XAbDate.Scribe,
        XAbLabel.Scribe,
        XAddressBookServerKind.Scribe,
        XAddressBookServerMember.Scribe,
        XPhoneticFirstName.Scribe,
        XPhoneticMiddleName.Scribe,
        XPhoneticLastName.Scribe,
        XSip.Scribe
    )

    fun registerAt(writer: ChainingTextWriter) {
        for (scribe in customScribes)
            writer.register(scribe)
    }

    fun registerAt(index: ScribeIndex) {
        for (scribe in customScribes)
            index.register(scribe)
    }

    fun registerAt(reader: VCardReader): VCardReader {
        for (scribe in customScribes)
            reader.scribeIndex.register(scribe)
        return reader
    }

}

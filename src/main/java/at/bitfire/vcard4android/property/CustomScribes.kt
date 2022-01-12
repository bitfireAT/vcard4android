/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.property

import ezvcard.io.chain.ChainingTextWriter
import ezvcard.io.json.JCardReader
import ezvcard.io.json.JCardWriter
import ezvcard.io.scribe.ScribeIndex
import ezvcard.io.text.VCardReader
import ezvcard.io.text.VCardWriter

object CustomScribes {

    /** list of all custom scribes (will be registered to readers/writers) **/
    private val customScribes = arrayOf(
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

    fun ScribeIndex.registerCustomScribes() {
        for (scribe in customScribes)
            register(scribe)
    }

    fun JCardReader.registerCustomScribes(): JCardReader {
        scribeIndex.registerCustomScribes()
        return this
    }

    fun JCardWriter.registerCustomScribes() =
        scribeIndex.registerCustomScribes()

    fun VCardReader.registerCustomScribes(): VCardReader {
        scribeIndex.registerCustomScribes()
        return this
    }

    fun VCardWriter.registerCustomScribes() =
        scribeIndex.registerCustomScribes()

}

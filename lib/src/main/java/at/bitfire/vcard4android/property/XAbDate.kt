/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.property

import ezvcard.io.scribe.DateOrTimePropertyScribe
import ezvcard.property.DateOrTimeProperty
import ezvcard.util.PartialDate
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.Temporal
import java.util.*

class XAbDate: DateOrTimeProperty {

    constructor(text: String?): super(text)
    constructor(date: Temporal?): super(date)
    constructor(partialDate: PartialDate?): super(partialDate)


    object Scribe : DateOrTimePropertyScribe<XAbDate>(XAbDate::class.java, "X-ABDATE") {

        override fun newInstance(text: String?) = XAbDate(text)
        override fun newInstance(temporal: Temporal?): XAbDate = XAbDate(temporal)
        override fun newInstance(partialDate: PartialDate?) = XAbDate(partialDate)

    }

}
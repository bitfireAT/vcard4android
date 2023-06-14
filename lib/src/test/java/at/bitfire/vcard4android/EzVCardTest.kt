/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android

import ezvcard.Ezvcard
import ezvcard.VCard
import ezvcard.VCardVersion
import ezvcard.property.Address
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.util.*

class EzVCardTest {

    @Test
    fun testREV_UTC() {
        val vCard = Ezvcard.parse("BEGIN:VCARD\r\n" +
                "VERSION:4.0\r\n" +
                "REV:20161218T201900Z\r\n" +
                "END:VCARD").first()
        assertNotNull(vCard.revision)
    }

    @Test
    fun testREV_UTC_Milliseconds() {
        val vCard = Ezvcard.parse("BEGIN:VCARD\r\n" +
                "VERSION:4.0\r\n" +
                "REV:2016-11-27T15:49:53.762Z\r\n" +
                "END:VCARD").first()
        assertNotNull(vCard.revision)
    }

    @Test
    fun testREV_WithoutTZ() {
        val vCard = Ezvcard.parse("BEGIN:VCARD\r\n" +
                "VERSION:4.0\r\n" +
                "REV:20161218T201900\r\n" +
                "END:VCARD").first()
        assertNotNull(vCard.revision)
    }

    @Test
    fun testREV_TZHourOffset() {
        val vCard = Ezvcard.parse("BEGIN:VCARD\r\n" +
                "VERSION:4.0\r\n" +
                "REV:20161218T201900-05\r\n" +
                "END:VCARD").first()
        assertNotNull(vCard.revision)
    }

    @Test
    fun testREV_TZHourAndMinOffset() {
        val vCard = Ezvcard.parse("BEGIN:VCARD\r\n" +
                "VERSION:4.0\r\n" +
                "REV:20161218T201900-0530\r\n" +
                "END:VCARD").first()
        assertNotNull(vCard.revision)
    }

    @Test
    fun testGenerateCaretNewline() {
        val vCard = VCard()
        vCard.addAddress(Address().apply {
            label = "Li^ne 1,1\n- \" -"
            streetAddress = "Line 1"
            country = "Line 2"
        })
        val str = Ezvcard .write(vCard)
                .version(VCardVersion.V4_0)
                .caretEncoding(true)
                .go().lines().filter { it.startsWith("ADR") }.first()
        //assertEquals("ADR;LABEL=\"Li^^ne 1,1^n- ^' -\":;;Line 1;;;;Line 2", str)
        assertEquals("ADR;LABEL=\"Li^^ne 1,1\\n- ^' -\":;;Line 1;;;;Line 2", str)
    }

}

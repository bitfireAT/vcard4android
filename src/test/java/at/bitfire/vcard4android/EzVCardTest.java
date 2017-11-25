/*
 * Copyright © Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.vcard4android;

import org.junit.Test;

import ezvcard.Ezvcard;
import ezvcard.VCard;

import static org.junit.Assert.assertNotNull;

public class EzVCardTest {

    @Test
    public void testREV_UTC() {
        VCard vCard = Ezvcard.parse("BEGIN:VCARD\r\n" +
                "VERSION:4.0\r\n" +
                "REV:20161218T201900Z\r\n" +
                "END:VCARD").first();
        assertNotNull(vCard.getRevision());
    }

    @Test
    public void testREV_UTC_Milliseconds() {
        VCard vCard = Ezvcard.parse("BEGIN:VCARD\r\n" +
                "VERSION:4.0\r\n" +
                "REV:2016-11-27T15:49:53.762Z\r\n" +
                "END:VCARD").first();
        assertNotNull(vCard.getRevision());
    }

    @Test
    public void testREV_WithoutTZ() {
        VCard vCard = Ezvcard.parse("BEGIN:VCARD\r\n" +
                "VERSION:4.0\r\n" +
                "REV:20161218T201900\r\n" +
                "END:VCARD").first();
        assertNotNull(vCard.getRevision());
    }

    @Test
    public void testREV_TZHourOffset() {
        VCard vCard = Ezvcard.parse("BEGIN:VCARD\r\n" +
                "VERSION:4.0\r\n" +
                "REV:20161218T201900-05\r\n" +
                "END:VCARD").first();
        assertNotNull(vCard.getRevision());
    }

    @Test
    public void testREV_TZHourAndMinOffset() {
        VCard vCard = Ezvcard.parse("BEGIN:VCARD\r\n" +
                "VERSION:4.0\r\n" +
                "REV:20161218T201900-0530\r\n" +
                "END:VCARD").first();
        assertNotNull(vCard.getRevision());
    }

}
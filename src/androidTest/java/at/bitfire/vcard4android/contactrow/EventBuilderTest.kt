/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds
import at.bitfire.vcard4android.Contact
import at.bitfire.vcard4android.LabeledProperty
import at.bitfire.vcard4android.property.XAbDate
import ezvcard.property.Anniversary
import ezvcard.property.Birthday
import ezvcard.util.PartialDate
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*

class EventBuilderTest {

    @Test
    fun testEmpty() {
        EventBuilder(Uri.EMPTY, null, Contact(), false).build().also { result ->
            assertEquals(0, result.size)
        }
    }


    @Test
    fun testStartDate_FullDate() {
        EventBuilder(Uri.EMPTY, null, Contact().apply {
            anniversary = Anniversary(Calendar.getInstance().apply {
                set(1984, /* zero-based */ 7, 20)
            })
        }, false).build().also { result ->
            assertEquals(1, result.size)
            assertEquals("1984-08-20", result[0].values[CommonDataKinds.Event.START_DATE])
            assertEquals(CommonDataKinds.Event.TYPE_ANNIVERSARY, result[0].values[CommonDataKinds.Event.TYPE])
        }
    }

    // TODO enable test as soon as https://github.com/mangstadt/ez-vcard/issues/113 is fixed
    /*@Test()
    fun testStartDate_PartialDate() {
        EventBuilder(Uri.EMPTY, null, Contact().apply {
            anniversary = Anniversary(PartialDate.builder()
                .date(20)
                .month(8)
                .build())
        }).build().also { result ->
            assertEquals(1, result.size)
            assertEquals("--08-20", result[0].values[CommonDataKinds.Event.START_DATE])
        }
    }*/


    @Test
    fun testBirthday_FullDate() {
        EventBuilder(Uri.EMPTY, null, Contact().apply {
            anniversary = Anniversary(Calendar.getInstance().apply {
                set(1984, /* zero-based */ 7, 20)
            })
        }, false).build().also { result ->
            assertEquals(1, result.size)
            assertEquals("1984-08-20", result[0].values[CommonDataKinds.Event.START_DATE])
        }
    }


    @Test
    fun testLabel() {
        EventBuilder(Uri.EMPTY, null, Contact().apply {
            customDates += LabeledProperty(XAbDate(PartialDate.builder()
                .date(20)
                .month(8)
                .build()), "Custom Event")
        }, false).build().also { result ->
            assertEquals(CommonDataKinds.Event.TYPE_CUSTOM, result[0].values[CommonDataKinds.Event.TYPE])
            assertEquals("Custom Event", result[0].values[CommonDataKinds.Event.LABEL])
        }
    }


    @Test
    fun testMimeType() {
        val c = Contact().apply {
            anniversary = Anniversary(Calendar.getInstance().apply {
                set(1984, /* zero-based */ 7, 20)
            })
        }
        EventBuilder(Uri.EMPTY, null, c, false).build().also { result ->
            assertEquals(CommonDataKinds.Event.CONTENT_ITEM_TYPE, result[0].values[CommonDataKinds.Event.MIMETYPE])
        }
    }


    @Test
    fun testType_Anniversary() {
        EventBuilder(Uri.EMPTY, null, Contact().apply {
            anniversary = Anniversary(Calendar.getInstance().apply {
                set(1984, /* zero-based */ 7, 20)
            })
        }, false).build().also { result ->
            assertEquals(CommonDataKinds.Event.TYPE_ANNIVERSARY, result[0].values[CommonDataKinds.Event.TYPE])
        }
    }

    @Test
    fun testType_Birthday() {
        EventBuilder(Uri.EMPTY, null, Contact().apply {
            birthDay = Birthday(Calendar.getInstance().apply {
                set(1984, /* zero-based */ 7, 20)
            })
        }, false).build().also { result ->
            assertEquals(CommonDataKinds.Event.TYPE_BIRTHDAY, result[0].values[CommonDataKinds.Event.TYPE])
        }
    }

    @Test
    fun testType_Other() {
        EventBuilder(Uri.EMPTY, null, Contact().apply {
            customDates += LabeledProperty(XAbDate(PartialDate.builder()
                .date(20)
                .month(8)
                .build()))
        }, false).build().also { result ->
            assertEquals(CommonDataKinds.Event.TYPE_OTHER, result[0].values[CommonDataKinds.Event.TYPE])
        }
    }

}
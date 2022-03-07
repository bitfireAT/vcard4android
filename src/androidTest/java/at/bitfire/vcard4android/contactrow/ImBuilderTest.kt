/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds
import at.bitfire.vcard4android.Contact
import at.bitfire.vcard4android.LabeledProperty
import ezvcard.parameter.ImppType
import ezvcard.property.Impp
import org.junit.Assert.assertEquals
import org.junit.Test

class ImBuilderTest {

    @Test
    fun testEmpty() {
        ImBuilder(Uri.EMPTY, null, Contact()).build().also { result ->
            assertEquals(0, result.size)
        }
    }


    @Test
    fun testHandle_Empty() {
        ImBuilder(Uri.EMPTY, null, Contact().apply {
            impps += LabeledProperty(Impp(""))
        }).build().also { result ->
            assertEquals(0, result.size)
        }
    }

    @Test
    fun testHandle_WithoutProtocol() {
        ImBuilder(Uri.EMPTY, null, Contact().apply {
            impps += LabeledProperty(Impp("test@example.com"))
        }).build().also { result ->
            assertEquals(0, result.size)
        }
    }

    // Android 12+
//    @Test
//    fun testHandle_WithCustomProtocol() {
//        ImBuilder(Uri.EMPTY, null, Contact().apply {
//            impps += LabeledProperty(Impp.xmpp("jabber@example.com"))
//        }).build().also { result ->
//            assertEquals(1, result.size)
//            assertEquals(CommonDataKinds.Im.PROTOCOL_CUSTOM, result[0].values[CommonDataKinds.Im.PROTOCOL])
//            assertEquals("xmpp", result[0].values[CommonDataKinds.Im.CUSTOM_PROTOCOL])
//            assertEquals("jabber@example.com", result[0].values[CommonDataKinds.Im.DATA])
//        }
//    }

    // Android 11 and below
    @Test
    @Suppress("DEPRECATION")
    fun testHandle_WithProtocol() {
        ImBuilder(Uri.EMPTY, null, Contact().apply {
            impps += LabeledProperty(Impp.xmpp("jabber@example.com"))
            impps += LabeledProperty(Impp.skype("skype-id"))
            impps += LabeledProperty(Impp("qq", "qq-id"))
        }).build().also { result ->
            assertEquals(3, result.size)
            assertEquals(CommonDataKinds.Im.PROTOCOL_JABBER, result[0].values[CommonDataKinds.Im.PROTOCOL])
            assertEquals("jabber@example.com", result[0].values[CommonDataKinds.Im.DATA])
            assertEquals(CommonDataKinds.Im.PROTOCOL_SKYPE, result[1].values[CommonDataKinds.Im.PROTOCOL])
            assertEquals("skype-id", result[1].values[CommonDataKinds.Im.DATA])
            assertEquals(CommonDataKinds.Im.PROTOCOL_QQ, result[2].values[CommonDataKinds.Im.PROTOCOL])
            assertEquals("qq-id", result[2].values[CommonDataKinds.Im.DATA])
        }
    }


    @Test
    fun testIgnoreSip() {
        ImBuilder(Uri.EMPTY, null, Contact().apply {
            impps += LabeledProperty(Impp("sip:voip@example.com"))
        }).build().also { result ->
            assertEquals(0, result.size)
        }
    }


    @Test
    fun testLabel() {
        ImBuilder(Uri.EMPTY, null, Contact().apply {
            impps += LabeledProperty(Impp.xmpp("jabber@example.com"), "Label")
        }).build().also { result ->
            assertEquals(CommonDataKinds.Im.TYPE_CUSTOM, result[0].values[CommonDataKinds.Im.TYPE])
            assertEquals("Label", result[0].values[CommonDataKinds.Im.LABEL])
        }
    }


    @Test
    fun testMimeType() {
        ImBuilder(Uri.EMPTY, null, Contact().apply {
            impps += LabeledProperty(Impp.xmpp("jabber@example.com"))
        }).build().also { result ->
            assertEquals(CommonDataKinds.Im.CONTENT_ITEM_TYPE, result[0].values[CommonDataKinds.Im.MIMETYPE])
        }
    }


    @Test
    fun testProtocol_Sip() {
        ImBuilder(Uri.EMPTY, null, Contact().apply {
            impps += LabeledProperty(Impp.sip("voip@example.com"))
        }).build().also { result ->
            // handled by SipAddressHandler
            assertEquals(0, result.size)
        }
    }


    @Test
    fun testType_Home() {
        ImBuilder(Uri.EMPTY, null, Contact().apply {
            impps += LabeledProperty(Impp.xmpp("jabber@example.com").apply {
                types.add(ImppType.HOME)
            })
        }).build().also { result ->
            assertEquals(CommonDataKinds.Im.TYPE_HOME, result[0].values[CommonDataKinds.Im.TYPE])
        }
    }

    @Test
    fun testType_NotInAndroid() {
        // some vCard type that is not supported by Android
        ImBuilder(Uri.EMPTY, null, Contact().apply {
            impps += LabeledProperty(Impp.xmpp("jabber@example.com").apply {
                types.add(ImppType.MOBILE)
            })
        }).build().also { result ->
            assertEquals(CommonDataKinds.Im.TYPE_OTHER, result[0].values[CommonDataKinds.Im.TYPE])
        }
    }

    @Test
    fun testType_Work() {
        ImBuilder(Uri.EMPTY, null, Contact().apply {
            impps += LabeledProperty(Impp.xmpp("jabber@example.com").apply {
                types.add(ImppType.WORK)
            })
        }).build().also { result ->
            assertEquals(CommonDataKinds.Im.TYPE_WORK, result[0].values[CommonDataKinds.Im.TYPE])
        }
    }

}
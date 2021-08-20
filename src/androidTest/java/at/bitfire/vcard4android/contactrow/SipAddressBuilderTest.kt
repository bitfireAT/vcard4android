package at.bitfire.vcard4android.contactrow

import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.SipAddress
import at.bitfire.vcard4android.Contact
import at.bitfire.vcard4android.LabeledProperty
import ezvcard.parameter.ImppType
import ezvcard.property.Impp
import org.junit.Assert.assertEquals
import org.junit.Test

class SipAddressBuilderTest {

    @Test
    fun testEmpty() {
        SipAddressBuilder(Uri.EMPTY, null, Contact()).build().also { result ->
            assertEquals(0, result.size)
        }
    }


    @Test
    fun testHandle_Empty() {
        SipAddressBuilder(Uri.EMPTY, null, Contact().apply {
            impps += LabeledProperty(Impp(""))
        }).build().also { result ->
            assertEquals(0, result.size)
        }
    }

    @Test
    fun testHandle_NotSip() {
        SipAddressBuilder(Uri.EMPTY, null, Contact().apply {
            impps += LabeledProperty(Impp.xmpp("test@example.com"))
        }).build().also { result ->
            assertEquals(0, result.size)
        }
    }

    @Test
    fun testHandle_Sip() {
        SipAddressBuilder(Uri.EMPTY, null, Contact().apply {
            impps += LabeledProperty(Impp.sip("voip@example.com"))
        }).build().also { result ->
            assertEquals(1, result.size)
            assertEquals("voip@example.com", result[0].values[SipAddress.SIP_ADDRESS])
        }
    }


    @Test
    fun testLabel() {
        SipAddressBuilder(Uri.EMPTY, null, Contact().apply {
            impps += LabeledProperty(Impp.sip("voip@example.com"), "Label")
        }).build().also { result ->
            assertEquals(SipAddress.TYPE_CUSTOM, result[0].values[SipAddress.TYPE])
            assertEquals("Label", result[0].values[SipAddress.LABEL])
        }
    }


    @Test
    fun testMimeType() {
        SipAddressBuilder(Uri.EMPTY, null, Contact().apply {
            impps += LabeledProperty(Impp.sip("voip@example.com"))
        }).build().also { result ->
            assertEquals(SipAddress.CONTENT_ITEM_TYPE, result[0].values[SipAddress.MIMETYPE])
        }
    }


    @Test
    fun testType_Home() {
        SipAddressBuilder(Uri.EMPTY, null, Contact().apply {
            impps += LabeledProperty(Impp.sip("voip@example.com").apply {
                types.add(ImppType.HOME)
            })
        }).build().also { result ->
            assertEquals(SipAddress.TYPE_HOME, result[0].values[SipAddress.TYPE])
        }
    }

    @Test
    fun testType_NotInAndroid() {
        // some vCard type that is not supported by Android
        SipAddressBuilder(Uri.EMPTY, null, Contact().apply {
            impps += LabeledProperty(Impp.sip("voip@example.com").apply {
                types.add(ImppType.MOBILE)
            })
        }).build().also { result ->
            assertEquals(SipAddress.TYPE_OTHER, result[0].values[SipAddress.TYPE])
        }
    }

    @Test
    fun testType_Work() {
        SipAddressBuilder(Uri.EMPTY, null, Contact().apply {
            impps += LabeledProperty(Impp.sip("voip@example.com").apply {
                types.add(ImppType.WORK)
            })
        }).build().also { result ->
            assertEquals(SipAddress.TYPE_WORK, result[0].values[SipAddress.TYPE])
        }
    }

}
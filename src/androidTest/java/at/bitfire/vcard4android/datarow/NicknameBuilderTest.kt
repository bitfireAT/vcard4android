package at.bitfire.vcard4android.datarow

import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds
import at.bitfire.vcard4android.LabeledProperty
import at.bitfire.vcard4android.property.CustomType
import ezvcard.property.Nickname
import org.junit.Assert.assertEquals
import org.junit.Test

/*class NicknameBuilderTest {

    @Test
    fun testEmpty() {
        NicknameBuilder(Uri.EMPTY, null, LabeledProperty(Nickname())).build().also { result ->
            assertEquals(0, result.size)
        }
    }


    @Test
    fun testLabel() {
        NicknameBuilder(Uri.EMPTY, null, LabeledProperty(Nickname().apply {
            values.add("Nick 1")
            type = CustomType.Nickname.SHORT_NAME       // will be ignored because there's a label
        }, "Label 1")).build().also { result ->
            assertEquals(1, result.size)
            assertEquals("Nick 1", result[0].values[CommonDataKinds.Nickname.NAME])
            assertEquals(CommonDataKinds.Nickname.TYPE_CUSTOM, result[0].values[CommonDataKinds.Nickname.TYPE])
            assertEquals("Label 1", result[0].values[CommonDataKinds.Nickname.LABEL])
        }
    }


    @Test
    fun testMimeType() {
        NicknameBuilder(Uri.EMPTY, null, LabeledProperty(Nickname().apply {
            values.add("Name 1")
        })).build().also { result ->
            assertEquals(CommonDataKinds.Nickname.CONTENT_ITEM_TYPE, result[0].values[CommonDataKinds.Nickname.MIMETYPE])
        }
    }


    @Test
    fun testType_Initials() {
        NicknameBuilder(Uri.EMPTY, null, LabeledProperty(Nickname().apply {
            values.add("Nick 1")
            type = CustomType.Nickname.INITIALS
        })).build().also { result ->
            assertEquals(1, result.size)
            assertEquals("Nick 1", result[0].values[CommonDataKinds.Nickname.NAME])
            assertEquals(CommonDataKinds.Nickname.TYPE_INITIALS, result[0].values[CommonDataKinds.Nickname.TYPE])
        }
    }

    @Test
    fun testType_MaidenName() {
        NicknameBuilder(Uri.EMPTY, null, LabeledProperty(Nickname().apply {
            values.add("Nick 1")
            type = CustomType.Nickname.MAIDEN_NAME
        })).build().also { result ->
            assertEquals(1, result.size)
            assertEquals("Nick 1", result[0].values[CommonDataKinds.Nickname.NAME])
            assertEquals(CommonDataKinds.Nickname.TYPE_MAIDEN_NAME, result[0].values[CommonDataKinds.Nickname.TYPE])
        }
    }

    @Test
    fun testType_ShortName() {
        NicknameBuilder(Uri.EMPTY, null, LabeledProperty(Nickname().apply {
            values.add("Nick 1")
            type = CustomType.Nickname.SHORT_NAME
        })).build().also { result ->
            assertEquals(1, result.size)
            assertEquals("Nick 1", result[0].values[CommonDataKinds.Nickname.NAME])
            assertEquals(CommonDataKinds.Nickname.TYPE_SHORT_NAME, result[0].values[CommonDataKinds.Nickname.TYPE])
        }
    }


    @Test
    fun testValue_TwoValues() {
        NicknameBuilder(Uri.EMPTY, null, LabeledProperty(Nickname().apply {
            values.add("Nick 1")
            values.add("Nick 2")
        })).build().also { result ->
            assertEquals(2, result.size)
            assertEquals("Nick 1", result[0].values[CommonDataKinds.Nickname.NAME])
            assertEquals("Nick 2", result[1].values[CommonDataKinds.Nickname.NAME])
        }
    }

}*/
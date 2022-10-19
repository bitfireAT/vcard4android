/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds
import at.bitfire.vcard4android.Contact
import at.bitfire.vcard4android.LabeledProperty
import at.bitfire.vcard4android.property.CustomType
import ezvcard.parameter.EmailType
import ezvcard.property.Email
import org.junit.Assert.assertEquals
import org.junit.Test

class EmailBuilderTest {

    @Test
    fun testEmpty() {
        EmailBuilder(Uri.EMPTY, null, Contact(), false).build().also { result ->
            assertEquals(0, result.size)
        }
    }


    @Test
    fun testAddress_Address() {
        EmailBuilder(Uri.EMPTY, null, Contact().apply {
            emails += LabeledProperty(Email("test@example.com"))
        }, false).build().also { result ->
            assertEquals(1, result.size)
            assertEquals("test@example.com", result[0].values[CommonDataKinds.Email.ADDRESS])
        }
    }

    @Test
    fun testAddress_Blank() {
        EmailBuilder(Uri.EMPTY, null, Contact().apply {
            emails += LabeledProperty(Email(""))
        }, false).build().also { result ->
            assertEquals(0, result.size)
        }
    }


    @Test
    fun testLabel() {
        EmailBuilder(Uri.EMPTY, null, Contact().apply {
            emails += LabeledProperty(Email("test@example.com"), "Label")
        }, false).build().also { result ->
            assertEquals("Label", result[0].values[CommonDataKinds.Email.LABEL])
        }
    }


    @Test
    fun testMimeType() {
        EmailBuilder(Uri.EMPTY, null, Contact().apply {
            emails += LabeledProperty(Email("test@example.com"))
        }, false).build().also { result ->
            assertEquals(CommonDataKinds.Email.CONTENT_ITEM_TYPE, result[0].values[CommonDataKinds.Email.MIMETYPE])
        }
    }


    @Test
    fun testPref_None() {
        EmailBuilder(Uri.EMPTY, null, Contact().apply {
            emails += LabeledProperty(Email("test@example.com"))
        }, false).build().also { result ->
            assertEquals(0, result[0].values[CommonDataKinds.Email.IS_PRIMARY])
        }
    }

    @Test
    fun testPref_1() {
        EmailBuilder(Uri.EMPTY, null, Contact().apply {
            emails += LabeledProperty(Email("test@example.com").apply {
                pref = 1
            })
        }, false).build().also { result ->
            assertEquals(1, result[0].values[CommonDataKinds.Email.IS_PRIMARY])
        }
    }


    @Test
    fun testTypeHome() {
        EmailBuilder(Uri.EMPTY, null, Contact().apply {
            emails += LabeledProperty(Email("test@example.com").apply {
                types.add(EmailType.HOME)
            })
        }, false).build().also { result ->
            assertEquals(CommonDataKinds.Email.TYPE_HOME, result[0].values[CommonDataKinds.Email.TYPE])
        }
    }

    @Test
    fun testTypeMobile() {
        EmailBuilder(Uri.EMPTY, null, Contact().apply {
            emails += LabeledProperty(Email("test@example.com").apply {
                types.add(CustomType.Email.MOBILE)
            })
        }, false).build().also { result ->
            assertEquals(CommonDataKinds.Email.TYPE_MOBILE, result[0].values[CommonDataKinds.Email.TYPE])
        }
    }

    @Test
    fun testTypeWork() {
        EmailBuilder(Uri.EMPTY, null, Contact().apply {
            emails += LabeledProperty(Email("test@example.com").apply {
                types.add(EmailType.WORK)
            })
        }, false).build().also { result ->
            assertEquals(CommonDataKinds.Email.TYPE_WORK, result[0].values[CommonDataKinds.Email.TYPE])
        }
    }

}
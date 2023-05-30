/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.content.ContentValues
import android.provider.ContactsContract.CommonDataKinds.Nickname
import at.bitfire.vcard4android.Contact
import at.bitfire.vcard4android.property.CustomType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NicknameHandlerTest {

    @Test
    fun testNickname_Empty() {
        val contact = Contact()
        NicknameHandler.handle(ContentValues().apply {
            putNull(Nickname.NAME)
        }, contact)
        assertNull(contact.nickName)
    }


    @Test
    fun testNickname_TypeCustom_NoLabel() {
        val contact = Contact()
        NicknameHandler.handle(ContentValues().apply {
            put(Nickname.NAME, "Nick")
            put(Nickname.TYPE, Nickname.TYPE_CUSTOM)
        }, contact)
        assertEquals("Nick", contact.nickName!!.property.values[0])
        assertNull(contact.nickName!!.label)
    }

    @Test
    fun testNickname_TypeCustom_WithLabel() {
        val contact = Contact()
        NicknameHandler.handle(ContentValues().apply {
            put(Nickname.NAME, "Nick")
            put(Nickname.TYPE, Nickname.TYPE_CUSTOM)
            put(Nickname.LABEL, "Label")
        }, contact)
        assertEquals("Nick", contact.nickName!!.property.values[0])
        assertEquals("Label", contact.nickName!!.label)
    }

    @Test
    fun testNickname_TypeInitials() {
        val contact = Contact()
        NicknameHandler.handle(ContentValues().apply {
            put(Nickname.NAME, "I1")
            put(Nickname.TYPE, Nickname.TYPE_INITIALS)
        }, contact)
        assertEquals("I1", contact.nickName!!.property.values[0])
        assertEquals(CustomType.Nickname.INITIALS, contact.nickName!!.property.type)
    }

    @Test
    fun testNickname_TypeMaidenName() {
        val contact = Contact()
        NicknameHandler.handle(ContentValues().apply {
            put(Nickname.NAME, "Mai Den")
            put(Nickname.TYPE, Nickname.TYPE_MAIDEN_NAME)
        }, contact)
        assertEquals("Mai Den", contact.nickName!!.property.values[0])
        assertEquals(CustomType.Nickname.MAIDEN_NAME, contact.nickName!!.property.type)
    }

    @Test
    fun testNickname_TypeShortName() {
        val contact = Contact()
        NicknameHandler.handle(ContentValues().apply {
            put(Nickname.NAME, "Short Name")
            put(Nickname.TYPE, Nickname.TYPE_SHORT_NAME)
        }, contact)
        assertEquals("Short Name", contact.nickName!!.property.values[0])
        assertEquals(CustomType.Nickname.SHORT_NAME, contact.nickName!!.property.type)
    }

}
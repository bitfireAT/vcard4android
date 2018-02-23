/*
 * Copyright © Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.vcard4android

import android.Manifest
import android.accounts.Account
import android.content.ContentProviderClient
import android.content.ContentValues
import android.provider.ContactsContract
import android.support.test.InstrumentationRegistry
import android.support.test.rule.GrantPermissionRule
import at.bitfire.vcard4android.impl.TestAddressBook
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AndroidAddressBookTest {

    @JvmField
    @Rule
    val permissionRule = GrantPermissionRule.grant(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)!!

    private val testAccount = Account("AndroidAddressBookTest", "at.bitfire.vcard4android")
    lateinit var provider: ContentProviderClient

	@Before
	fun connect() {
        val context = InstrumentationRegistry.getContext()
		provider = context.contentResolver.acquireContentProviderClient(ContactsContract.AUTHORITY)
        assertNotNull(provider)
    }

	@After
	fun disconnect() {
		provider.release()
    }


    @Test
	fun testSettings() {
		val addressBook = TestAddressBook(testAccount, provider)

        var values = ContentValues()
        values.put(ContactsContract.Settings.SHOULD_SYNC, false)
        values.put(ContactsContract.Settings.UNGROUPED_VISIBLE, false)
        addressBook.updateSettings(values)
        values = addressBook.getSettings()
        assertFalse(values.getAsInteger(ContactsContract.Settings.SHOULD_SYNC) != 0)
        assertFalse(values.getAsInteger(ContactsContract.Settings.UNGROUPED_VISIBLE) != 0)

        values = ContentValues()
        values.put(ContactsContract.Settings.SHOULD_SYNC, true)
        values.put(ContactsContract.Settings.UNGROUPED_VISIBLE, true)
        addressBook.updateSettings(values)
        values = addressBook.getSettings()
        assertTrue(values.getAsInteger(ContactsContract.Settings.SHOULD_SYNC) != 0)
        assertTrue(values.getAsInteger(ContactsContract.Settings.UNGROUPED_VISIBLE) != 0)
    }

    @Test
    fun testSyncState() {
		val addressBook = TestAddressBook(testAccount, provider)

        addressBook.writeSyncState(ByteArray(0))
        assertEquals(0, addressBook.readSyncState()!!.size)

        val random = byteArrayOf(1, 2, 3, 4, 5)
        addressBook.writeSyncState(random)
        assertArrayEquals(random, addressBook.readSyncState())
    }

}

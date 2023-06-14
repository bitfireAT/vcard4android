/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android

import android.Manifest
import android.accounts.Account
import android.content.ContentProviderClient
import android.content.ContentValues
import android.provider.ContactsContract
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import at.bitfire.vcard4android.impl.TestAddressBook
import org.junit.*
import org.junit.Assert.*

class AndroidAddressBookTest {

    companion object {
        @JvmField
        @ClassRule
        val permissionRule = GrantPermissionRule.grant(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)!!

        private val testAccount = Account("AndroidAddressBookTest", "at.bitfire.vcard4android")
        private lateinit var provider: ContentProviderClient

        @BeforeClass
        @JvmStatic
        fun connect() {
            val context = InstrumentationRegistry.getInstrumentation().context
            provider = context.contentResolver.acquireContentProviderClient(ContactsContract.AUTHORITY)!!
            assertNotNull(provider)
        }

        @BeforeClass
        @JvmStatic
        fun disconnect() {
            @Suppress("DEPRECATION")
            provider.release()
        }
    }


    @Test
	fun testSettings() {
		val addressBook = TestAddressBook(testAccount, provider)

        var values = ContentValues()
        values.put(ContactsContract.Settings.SHOULD_SYNC, false)
        values.put(ContactsContract.Settings.UNGROUPED_VISIBLE, false)
        addressBook.settings = values
        values = addressBook.settings
        assertFalse(values.getAsInteger(ContactsContract.Settings.SHOULD_SYNC) != 0)
        assertFalse(values.getAsInteger(ContactsContract.Settings.UNGROUPED_VISIBLE) != 0)

        values = ContentValues()
        values.put(ContactsContract.Settings.SHOULD_SYNC, true)
        values.put(ContactsContract.Settings.UNGROUPED_VISIBLE, true)
        addressBook.settings = values
        values = addressBook.settings
        assertTrue(values.getAsInteger(ContactsContract.Settings.SHOULD_SYNC) != 0)
        assertTrue(values.getAsInteger(ContactsContract.Settings.UNGROUPED_VISIBLE) != 0)
    }

    @Test
    fun testSyncState() {
		val addressBook = TestAddressBook(testAccount, provider)

        addressBook.syncState = ByteArray(0)
        assertEquals(0, addressBook.syncState!!.size)

        val random = byteArrayOf(1, 2, 3, 4, 5)
        addressBook.syncState = random
        assertArrayEquals(random, addressBook.syncState)
    }

}

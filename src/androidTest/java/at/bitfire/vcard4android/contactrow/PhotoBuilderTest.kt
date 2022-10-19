/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.Manifest
import android.accounts.Account
import android.content.ContentProviderClient
import android.content.ContentUris
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.RawContacts
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import at.bitfire.vcard4android.AndroidContact
import at.bitfire.vcard4android.Contact
import at.bitfire.vcard4android.impl.TestAddressBook
import org.apache.commons.io.IOUtils
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import kotlin.random.Random

class PhotoBuilderTest {

    companion object {
        @JvmField
        @ClassRule
        val permissionRule = GrantPermissionRule.grant(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)!!

        private val testAccount = Account("AndroidContactTest", "at.bitfire.vcard4android")

        val testContext = InstrumentationRegistry.getInstrumentation().context
        private lateinit var provider: ContentProviderClient
        private lateinit var addressBook: TestAddressBook

        @BeforeClass
        @JvmStatic
        fun connect() {
            provider = testContext.contentResolver.acquireContentProviderClient(ContactsContract.AUTHORITY)!!
            assertNotNull(provider)

            addressBook = TestAddressBook(testAccount, provider)
        }

        @BeforeClass
        @JvmStatic
        fun disconnect() {
            @Suppress("DEPRECATION")
            provider.release()
        }
    }


    @Test
    fun testBuild_NoPhoto() {
        PhotoBuilder(Uri.EMPTY, null, Contact(), false).build().also { result ->
            assertEquals(0, result.size)
        }
    }

    @Test
    fun testBuild_Photo() {
        val blob = ByteArray(1024) { Random.nextInt().toByte() }
        PhotoBuilder(Uri.EMPTY, null, Contact().apply {
            photo = blob
        }, false).build().also { result ->
            // no row because photos have to be inserted with a separate call to insertPhoto()
            assertEquals(0, result.size)
        }
    }


    @Test
    fun testInsertPhoto() {
        val contact = AndroidContact(addressBook, Contact().apply { displayName = "Contact with photo" }, null, null)
        val contactUri = contact.add()
        val rawContactId = ContentUris.parseId(contactUri)

        try {
            val photo = IOUtils.resourceToByteArray("/large.jpg")
            val photoUri = PhotoBuilder.insertPhoto(provider, testAccount, rawContactId, photo)
            assertNotNull(photoUri)

            // the photo is processed and often resized by the contacts provider
            val contact2 = addressBook.findContactById(rawContactId)
            val photo2 = contact2.getContact().photo!!

            // verify that the image is in JPEG format (some Samsung devices seem to save as PNG)
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeByteArray(photo2, 0, photo2.size, options)
            assertEquals("image/jpeg", options.outMimeType)

            // verify that contact is not dirty
            provider.query(
                ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
                arrayOf(RawContacts.DIRTY),
                null, null, null
            )!!.use { cursor ->
                assertTrue(cursor.moveToNext())
                assertEquals(0, cursor.getInt(0))
            }
        } finally {
            contact.delete()
        }
    }

    @Test
    fun testInsertPhoto_Invalid() {
        val contact = AndroidContact(addressBook, Contact().apply { displayName = "Contact with photo" }, null, null)
        contact.add()
        try {
            assertNull(PhotoBuilder.insertPhoto(provider, testAccount, contact.id!!, ByteArray(100) /* invalid photo  */))
        } finally {
            contact.delete()
        }
    }

}
/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.accounts.Account
import android.content.ContentProviderClient
import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Photo
import android.provider.ContactsContract.RawContacts
import at.bitfire.vcard4android.BatchOperation
import at.bitfire.vcard4android.Constants
import at.bitfire.vcard4android.Contact
import at.bitfire.vcard4android.Utils.asSyncAdapter
import java.util.logging.Level

class PhotoBuilder(dataRowUri: Uri, rawContactId: Long?, contact: Contact)
    : DataRowBuilder(Factory.mimeType(), dataRowUri, rawContactId, contact) {

    companion object {

        fun insertPhoto(provider: ContentProviderClient, account: Account, rawContactId: Long, data: ByteArray) {
            val uri = RawContacts.CONTENT_URI.buildUpon()
                .appendPath(rawContactId.toString())
                .appendPath(RawContacts.DisplayPhoto.CONTENT_DIRECTORY)
                .build()
            Constants.log.log(Level.FINE, "Writing photo to $uri (${data.size} bytes)")
            provider.openAssetFile(uri, "w")?.use { fd ->
                fd.createOutputStream()?.use { os ->
                    os.write(data)
                }
            }

            // photo is now processed in the background; wait until it is available
            var photoUri: Uri? = null
            for (i in 1..50) {      // wait max. 50x100 ms = 5 seconds
                val dataRowUri = RawContacts.CONTENT_URI.buildUpon()
                    .appendPath(rawContactId.toString())
                    .appendPath(RawContacts.Data.CONTENT_DIRECTORY)
                    .build()
                provider.query(dataRowUri, arrayOf(Photo.PHOTO_URI), "${RawContacts.Data.MIMETYPE}=?", arrayOf(Photo.CONTENT_ITEM_TYPE), null)?.use { cursor ->
                    if (cursor.moveToNext())
                        cursor.getString(0)?.let { uriStr ->
                            photoUri = Uri.parse(uriStr)
                        }
                }
                if (photoUri != null)
                    break
                Thread.sleep(100)
            }

            if (photoUri != null) {
                Constants.log.log(Level.FINE, "Photo has been inserted: $photoUri")

                // reset dirty flag
                val notDirty = ContentValues(1)
                notDirty.put(RawContacts.DIRTY, 0)
                val rawContactUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId).asSyncAdapter(account)
                provider.update(rawContactUri, notDirty, null, null)
            } else
                Constants.log.log(Level.WARNING, "Couldn't insert photo")
        }

    }

    override fun build(): List<BatchOperation.CpoBuilder> =
        emptyList()     // data row must be inserted by calling insertPhoto()


    object Factory: DataRowBuilder.Factory<PhotoBuilder> {
        override fun mimeType() = Photo.CONTENT_ITEM_TYPE
        override fun newInstance(dataRowUri: Uri, rawContactId: Long?, contact: Contact) =
            PhotoBuilder(dataRowUri, rawContactId, contact)
    }

}
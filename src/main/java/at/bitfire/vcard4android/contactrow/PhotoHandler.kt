/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.content.ContentProviderClient
import android.content.ContentUris
import android.content.ContentValues
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Photo
import at.bitfire.vcard4android.Constants
import at.bitfire.vcard4android.Contact
import org.apache.commons.io.IOUtils
import java.io.IOException
import java.util.logging.Level

class PhotoHandler(val provider: ContentProviderClient?): DataRowHandler() {

    override fun forMimeType() = Photo.CONTENT_ITEM_TYPE

    override fun handle(values: ContentValues, contact: Contact) {
        super.handle(values, contact)

        values.getAsLong(Photo.PHOTO_FILE_ID)?.let { photoId ->
            val photoUri = ContentUris.withAppendedId(ContactsContract.DisplayPhoto.CONTENT_URI, photoId)
            try {
                provider?.openAssetFile(photoUri, "r")?.let { file ->
                    file.createInputStream().use {
                        contact.photo = IOUtils.toByteArray(it)
                    }
                }
            } catch(e: IOException) {
                Constants.log.log(Level.WARNING, "Couldn't read local contact photo file", e)
            }
        }

        if (contact.photo == null)
            contact.photo = values.getAsByteArray(Photo.PHOTO)
    }

}
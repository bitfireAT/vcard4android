package at.bitfire.vcard4android.datavalues

import android.content.ContentProviderClient
import android.content.ContentUris
import android.content.ContentValues
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Photo
import at.bitfire.vcard4android.Constants
import at.bitfire.vcard4android.Contact
import at.bitfire.vcard4android.datarow.DataRowHandler
import org.apache.commons.io.IOUtils
import java.io.IOException
import java.util.logging.Level

class PhotoHandler(val provider: ContentProviderClient?): DataRowHandler() {

    override fun forMimeType() = Photo.CONTENT_ITEM_TYPE

    override fun handle(values: ContentValues, contact: Contact) {
        super.handle(values, contact)

        val photoId = values.getAsLong(Photo.PHOTO_FILE_ID)
        if (photoId != null) {
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
        } else
            contact.photo = values.getAsByteArray(Photo.PHOTO)
    }

}
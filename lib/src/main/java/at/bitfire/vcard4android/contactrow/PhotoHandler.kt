/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.content.ContentProviderClient
import android.content.ContentUris
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Photo
import at.bitfire.vcard4android.Constants
import at.bitfire.vcard4android.Contact
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.logging.Level

class PhotoHandler(val provider: ContentProviderClient?): DataRowHandler() {

    companion object {

        /**
         * Converts non-JPEG images to JPEG (compression: 75).
         * Does nothing if image is already in JPEG format.
         *
         * @param photo   image in format that can be read by [BitmapFactory]
         * @param quality JPEG quality (ignored when [photo] is already in JPEG format)
         * @return        same image in JPEG format or null if image couldn't be converted
         */
        fun convertToJpeg(photo: ByteArray, quality: Int): ByteArray? {
            val opts = BitmapFactory.Options()
            opts.inJustDecodeBounds = true
            BitmapFactory.decodeByteArray(photo, 0, photo.size, opts)
            if (opts.outMimeType == "image/jpeg")
            // image is already a JPEG
                return photo

            // decode image
            val bmp = BitmapFactory.decodeByteArray(photo, 0, photo.size)
            if (bmp == null)
                return null

            // compress as JPEG
            val result = ByteArrayOutputStream()
            if (bmp.compress(Bitmap.CompressFormat.JPEG, quality, result))
                return result.toByteArray()

            return null
        }

    }


    override fun forMimeType() = Photo.CONTENT_ITEM_TYPE

    override fun handle(values: ContentValues, contact: Contact) {
        super.handle(values, contact)

        /* JPEG qualities are taken from
           https://android.googlesource.com/platform/packages/providers/ContactsProvider.git/+/refs/heads/android12-release/src/com/android/providers/contacts/PhotoProcessor.java

           Compression for display photos: 75
           Compression for thumbnails that don't have a full-size photo: 95
        */

        values.getAsLong(Photo.PHOTO_FILE_ID)?.let { photoId ->
            val photoUri = ContentUris.withAppendedId(ContactsContract.DisplayPhoto.CONTENT_URI, photoId)
            try {
                provider?.openAssetFile(photoUri, "r")?.let { file ->
                    file.createInputStream().use {
                        // Samsung Android 12 bug: they return a PNG image with MIME type image/jpeg
                        convertToJpeg(IOUtils.toByteArray(it), 75)?.let { jpeg ->
                            Constants.log.log(Level.FINE, "Got high-res contact photo (${FileUtils.byteCountToDisplaySize(jpeg.size.toLong())})")
                            contact.photo = jpeg
                        }
                    }
                }
            } catch(e: IOException) {
                Constants.log.log(Level.WARNING, "Couldn't read local contact photo file", e)
            }
        }

        if (contact.photo == null) {
            values.getAsByteArray(Photo.PHOTO)?.let { thumbnail ->
                // Samsung Android 12 bug: even the thumbnail is a PNG image
                convertToJpeg(thumbnail, 95)?.let { jpeg ->
                    Constants.log.log(Level.FINE, "Got contact photo thumbnail (${FileUtils.byteCountToDisplaySize(jpeg.size.toLong())})")
                    contact.photo = jpeg
                }
            }
        }
    }

}
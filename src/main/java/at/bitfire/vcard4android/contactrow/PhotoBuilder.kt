package at.bitfire.vcard4android.contactrow

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Photo
import at.bitfire.vcard4android.BatchOperation
import at.bitfire.vcard4android.Constants
import at.bitfire.vcard4android.Contact
import java.io.ByteArrayOutputStream
import java.util.*

class PhotoBuilder(dataRowUri: Uri, rawContactId: Long?, contact: Contact)
    : DataRowBuilder(Factory.mimeType(), dataRowUri, rawContactId, contact) {

    companion object {
        val MAX_PHOTO_BLOB_SIZE = 950*1024    // IPC limit 1 MB, minus 50 kB for the protocol itself = 950 kB
        val MAX_RESIZE_PASSES = 10
    }

    override fun build(): List<BatchOperation.CpoBuilder> {
        // The following approach would be correct, but it doesn't work:
        // the ContactsProvider handler will process the image in background and update
        // the raw contact with the new photo ID when it's finished, setting it to dirty again!
        // See https://code.google.com/p/android/issues/detail?id=226875

        /*Uri photoUri = addressBook.syncAdapterURI(Uri.withAppendedPath(
                ContentUris.withAppendedId(RawContacts.CONTENT_URI, id),
                RawContacts.DisplayPhoto.CONTENT_DIRECTORY));
        Constants.log.debug("Setting local photo " + photoUri);
        try {
            @Cleanup AssetFileDescriptor fd = addressBook.provider.openAssetFile(photoUri, "w");
            @Cleanup OutputStream stream = fd.createOutputStream();
            if (stream != null)
                stream.write(photo);
            else
                Constants.log.warn("Couldn't create local contact photo file");
        } catch (IOException|RemoteException e) {
            Constants.log.warn("Couldn't write local contact photo file", e);
        }*/

        val result = LinkedList<BatchOperation.CpoBuilder>()
        contact.photo?.let { photo ->
            val resized = resizeIfNecessary(photo)
            if (resized != null)
                result += newDataRow().withValue(Photo.PHOTO, resized)
        }
        return result
    }

    private fun resizeIfNecessary(blob: ByteArray): ByteArray? {
        if (blob.size > MAX_PHOTO_BLOB_SIZE) {
            Constants.log.fine("Photo larger than $MAX_PHOTO_BLOB_SIZE bytes, resizing")

            val bitmap = BitmapFactory.decodeByteArray(blob, 0, blob.size)
            if (bitmap == null) {
                Constants.log.warning("Image decoding failed")
                return null
            }

            var size = Math.min(bitmap.width, bitmap.height).toFloat()
            var resized: ByteArray = blob
            var count = 0
            var quality = 98
            do {
                if (++count > MAX_RESIZE_PASSES) {
                    Constants.log.warning("Couldn't resize photo within $MAX_RESIZE_PASSES passes")
                    return null
                }

                val sizeInt = size.toInt()
                val thumb = ThumbnailUtils.extractThumbnail(bitmap, sizeInt, sizeInt)
                val baos = ByteArrayOutputStream()
                if (thumb.compress(Bitmap.CompressFormat.JPEG, quality, baos))
                    resized = baos.toByteArray()

                size *= .9f
                quality--
            } while (resized.size >= MAX_PHOTO_BLOB_SIZE)

            return resized

        } else
            return blob
    }


    object Factory: DataRowBuilder.Factory<PhotoBuilder> {
        override fun mimeType() = Photo.CONTENT_ITEM_TYPE
        override fun newInstance(dataRowUri: Uri, rawContactId: Long?, contact: Contact) =
            PhotoBuilder(dataRowUri, rawContactId, contact)
    }

}
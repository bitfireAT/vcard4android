package at.bitfire.vcard4android.datarow

import android.content.ContentProviderClient
import android.content.ContentValues
import androidx.annotation.CallSuper
import at.bitfire.vcard4android.Contact

/**
 * Handler for a raw contact's data row.
 *
 * @param provider      content provider client that has been used to fetch the data row;
 *                      may be used by the handler to fetch further data (like a photo)
 *
 */
abstract class DataRowHandler {

    abstract fun forMimeType(): String

    /**
     * Processes the given data.
     *
     * @param values   values to process
     * @param contact  contact that is modified according to the values
     */
    @CallSuper
    open fun handle(values: ContentValues, contact: Contact) {
        // remove empty strings
        val it = values.keySet().iterator()
        while (it.hasNext()) {
            val obj = values[it.next()]
            if (obj is String && obj.isEmpty())
                it.remove()
        }
    }

}
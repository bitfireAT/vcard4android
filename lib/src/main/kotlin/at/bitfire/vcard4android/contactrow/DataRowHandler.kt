/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.content.ContentValues
import androidx.annotation.CallSuper
import at.bitfire.vcard4android.Contact

/**
 * Handler for a raw contact's data row.
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
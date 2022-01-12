/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android

import android.content.ContentValues
import android.database.Cursor
import android.database.DatabaseUtils

object Utils {

    fun Cursor.toContentValues(): ContentValues {
        val values = ContentValues(columnCount)
        DatabaseUtils.cursorRowToContentValues(this, values)
        return values
    }

}
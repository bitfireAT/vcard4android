/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android

import android.content.ContentValues
import android.database.Cursor
import android.database.DatabaseUtils
import android.icu.text.Normalizer2
import android.os.Build
import org.apache.commons.lang3.StringUtils
import java.text.Normalizer

object Utils {

    fun Cursor.toContentValues(): ContentValues {
        val values = ContentValues(columnCount)
        DatabaseUtils.cursorRowToContentValues(this, values)
        return values
    }

    fun String.normalizeNFD(): String =
        if (Build.VERSION.SDK_INT >= 24)
            Normalizer2.getNFDInstance().normalize(this)
        else
            Normalizer.normalize(this, Normalizer.Form.NFD)

}
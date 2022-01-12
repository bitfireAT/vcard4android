/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.content.ContentValues
import android.provider.ContactsContract.CommonDataKinds.Event
import at.bitfire.vcard4android.Constants
import at.bitfire.vcard4android.Contact
import at.bitfire.vcard4android.LabeledProperty
import at.bitfire.vcard4android.property.XAbDate
import ezvcard.property.Anniversary
import ezvcard.property.Birthday
import ezvcard.util.PartialDate
import org.apache.commons.lang3.StringUtils
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Level

object EventHandler: DataRowHandler() {

    override fun forMimeType() = Event.CONTENT_ITEM_TYPE

    override fun handle(values: ContentValues, contact: Contact) {
        super.handle(values, contact)

        val dateStr = values.getAsString(Event.START_DATE) ?: return
        var full: Date? = null
        var partial: PartialDate? = null
        val fullFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        try {
            full = fullFormat.parse(dateStr)
        } catch(e: ParseException) {
            try {
                partial = PartialDate.parse(dateStr)
            } catch (e: IllegalArgumentException) {
                Constants.log.log(Level.WARNING, "Couldn't parse birthday/anniversary date from database", e)
            }
        }

        if (full != null || partial != null)
            when (values.getAsInteger(Event.TYPE)) {
                Event.TYPE_ANNIVERSARY ->
                    contact.anniversary = if (full != null) Anniversary(full) else Anniversary(partial)
                Event.TYPE_BIRTHDAY ->
                    contact.birthDay = if (full != null) Birthday(full) else Birthday(partial)
                /* Event.TYPE_OTHER,
                Event.TYPE_CUSTOM */
                else -> {
                    val abDate = if (full != null) XAbDate(full) else XAbDate(partial)
                    val label = StringUtils.trimToNull(values.getAsString(Event.LABEL))
                    contact.customDates += LabeledProperty(abDate, label)
                }
            }
    }

}
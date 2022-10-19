/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.net.Uri
import at.bitfire.vcard4android.BatchOperation
import at.bitfire.vcard4android.Contact
import org.junit.Assert.*
import org.junit.Test
import java.util.*

class DataRowBuilderTest {

    @Test
    fun newDataRow_readOnly() {
        val list = TestDataRowBuilder(Uri.EMPTY, 0, Contact(), true).build()
        assertEquals(1, list[0].values["is_read_only"])
    }

    @Test
    fun newDataRow_notReadOnly() {
        val list = TestDataRowBuilder(Uri.EMPTY, 0, Contact(), false).build()
        assertEquals(null, list[0].values["is_read_only"]) // ensure value was not set
    }

    class TestDataRowBuilder(dataRowUri: Uri, rawContactId: Long?, contact: Contact, readOnly: Boolean)
        : DataRowBuilder("", dataRowUri, rawContactId, contact, readOnly) {
        override fun build(): List<BatchOperation.CpoBuilder> {
            return LinkedList<BatchOperation.CpoBuilder>().apply {
                add(newDataRow())
            }
        }
    }

}
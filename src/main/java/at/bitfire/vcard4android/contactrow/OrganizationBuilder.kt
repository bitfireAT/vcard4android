/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Organization
import at.bitfire.vcard4android.BatchOperation
import at.bitfire.vcard4android.Contact
import org.apache.commons.lang3.StringUtils
import java.util.*

class OrganizationBuilder(dataRowUri: Uri, rawContactId: Long?, contact: Contact, readOnly: Boolean)
    : DataRowBuilder(Factory.mimeType(), dataRowUri, rawContactId, contact, readOnly) {

    override fun build(): List<BatchOperation.CpoBuilder> {
        var company: String? = null
        var department: String? = null
        contact.organization?.let {
            val org = it.values.iterator()
            if (org.hasNext())
                company = org.next()

            val depts = LinkedList<String>()
            while (org.hasNext())
                depts += org.next()
            department = StringUtils.trimToNull(depts.joinToString(" / "))
        }

        if (company == null && department == null && contact.jobTitle == null && contact.jobDescription == null)
            return emptyList()

        return listOf(newDataRow().apply {
            withValue(Organization.COMPANY, company)
            withValue(Organization.DEPARTMENT, department)
            withValue(Organization.TITLE, contact.jobTitle)
            withValue(Organization.JOB_DESCRIPTION, contact.jobDescription)
        })
    }


    object Factory: DataRowBuilder.Factory<OrganizationBuilder> {
        override fun mimeType() = Organization.CONTENT_ITEM_TYPE
        override fun newInstance(dataRowUri: Uri, rawContactId: Long?, contact: Contact, readOnly: Boolean) =
            OrganizationBuilder(dataRowUri, rawContactId, contact, readOnly)
    }

}
package at.bitfire.vcard4android.contactrow

import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Website
import at.bitfire.vcard4android.BatchOperation
import at.bitfire.vcard4android.Contact
import at.bitfire.vcard4android.property.CustomType
import java.util.*

class WebsiteBuilder(dataRowUri: Uri, rawContactId: Long?, contact: Contact)
    : DataRowBuilder(Factory.mimeType(), dataRowUri, rawContactId, contact) {

    override fun build(): List<BatchOperation.CpoBuilder> {
        val result = LinkedList<BatchOperation.CpoBuilder>()
        for (labeledUrl in contact.urls) {
            val url = labeledUrl.property
            if (url.value.isNullOrBlank())
                continue

            val typeCode: Int
            var typeLabel: String? = null
            if (labeledUrl.label != null) {
                typeCode = Website.TYPE_CUSTOM
                typeLabel = labeledUrl.label
            } else
                typeCode = when (url.type?.lowercase()) {
                    CustomType.Url.TYPE_HOMEPAGE -> Website.TYPE_HOMEPAGE
                    CustomType.Url.TYPE_BLOG ->     Website.TYPE_BLOG
                    CustomType.Url.TYPE_PROFILE ->  Website.TYPE_PROFILE
                    CustomType.Url.TYPE_FTP ->      Website.TYPE_FTP
                    CustomType.HOME ->              Website.TYPE_HOME
                    CustomType.WORK ->              Website.TYPE_WORK
                    else ->                         Website.TYPE_OTHER
                }

            result += newDataRow()
                .withValue(Website.URL, url.value)
                .withValue(Website.TYPE, typeCode)
                .withValue(Website.LABEL, typeLabel)
        }
        return result
    }


    object Factory: DataRowBuilder.Factory<WebsiteBuilder> {
        override fun mimeType() = Website.CONTENT_ITEM_TYPE
        override fun newInstance(dataRowUri: Uri, rawContactId: Long?, contact: Contact) =
            WebsiteBuilder(dataRowUri, rawContactId, contact)
    }

}
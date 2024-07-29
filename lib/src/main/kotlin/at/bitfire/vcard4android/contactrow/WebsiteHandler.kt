/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.content.ContentValues
import android.provider.ContactsContract.CommonDataKinds.Website
import at.bitfire.vcard4android.Contact
import at.bitfire.vcard4android.LabeledProperty
import at.bitfire.vcard4android.property.CustomType
import ezvcard.property.Url

object WebsiteHandler: DataRowHandler() {

    override fun forMimeType() = Website.CONTENT_ITEM_TYPE

    override fun handle(values: ContentValues, contact: Contact) {
        super.handle(values, contact)

        val url = Url(values.getAsString(Website.URL) ?: return)
        val labeledUrl = LabeledProperty(url)

        when (values.getAsInteger(Website.TYPE)) {
            Website.TYPE_HOMEPAGE ->
                url.type = CustomType.Url.TYPE_HOMEPAGE
            Website.TYPE_BLOG ->
                url.type = CustomType.Url.TYPE_BLOG
            Website.TYPE_PROFILE ->
                url.type = CustomType.Url.TYPE_PROFILE
            Website.TYPE_HOME ->
                url.type = CustomType.HOME
            Website.TYPE_WORK ->
                url.type = CustomType.WORK
            Website.TYPE_FTP ->
                url.type = CustomType.Url.TYPE_FTP
            Website.TYPE_CUSTOM ->
                values.getAsString(Website.LABEL)?.let {
                    labeledUrl.label = it
                }
        }
        contact.urls += labeledUrl
    }

}
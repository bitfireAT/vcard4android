/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Relation
import at.bitfire.vcard4android.BatchOperation
import at.bitfire.vcard4android.Contact
import at.bitfire.vcard4android.property.CustomType
import ezvcard.parameter.RelatedType
import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.WordUtils
import java.util.*

class RelationBuilder(dataRowUri: Uri, rawContactId: Long?, contact: Contact, readOnly: Boolean)
    : DataRowBuilder(Factory.mimeType(), dataRowUri, rawContactId, contact, readOnly) {

    override fun build(): List<BatchOperation.CpoBuilder> {
        val result = LinkedList<BatchOperation.CpoBuilder>()
        for (related in contact.relations) {
            val name = StringUtils.trimToNull(related.text) ?: StringUtils.trimToNull(related.uri)
            if (name.isNullOrBlank())
                continue

            val typeCode = when {
                // specific Android types (not defined in RFC 6350)
                related.types.contains(CustomType.Related.ASSISTANT) -> Relation.TYPE_ASSISTANT
                related.types.contains(CustomType.Related.BROTHER) -> Relation.TYPE_BROTHER
                related.types.contains(CustomType.Related.DOMESTIC_PARTNER) -> Relation.TYPE_DOMESTIC_PARTNER
                related.types.contains(CustomType.Related.FATHER) -> Relation.TYPE_FATHER
                related.types.contains(CustomType.Related.MANAGER) -> Relation.TYPE_MANAGER
                related.types.contains(CustomType.Related.MOTHER) -> Relation.TYPE_MOTHER
                related.types.contains(CustomType.Related.PARTNER) -> Relation.TYPE_PARTNER
                related.types.contains(CustomType.Related.REFERRED_BY) -> Relation.TYPE_REFERRED_BY
                related.types.contains(CustomType.Related.SISTER) -> Relation.TYPE_SISTER

                // standard types (defined in RFC 6350) supported by Android
                related.types.contains(RelatedType.CHILD) -> Relation.TYPE_CHILD
                related.types.contains(RelatedType.FRIEND) -> Relation.TYPE_FRIEND
                related.types.contains(RelatedType.KIN) -> Relation.TYPE_RELATIVE
                related.types.contains(RelatedType.PARENT) -> Relation.TYPE_PARENT
                related.types.contains(RelatedType.SPOUSE) -> Relation.TYPE_SPOUSE

                // other standard types are set as TYPE_CUSTOM
                else -> Relation.TYPE_CUSTOM
            }

            val builder = newDataRow()
                .withValue(Relation.NAME, name)
                .withValue(Relation.TYPE, typeCode)

            if (typeCode == Relation.TYPE_CUSTOM) {
                if (related.types.isEmpty())
                    related.types += CustomType.Related.OTHER

                val types = related.types.map { type -> WordUtils.capitalize(type.value) }
                val typesStr = types.joinToString(", ")
                builder.withValue(Relation.LABEL, typesStr)
            }

            result += builder
        }
        return result
    }


    object Factory: DataRowBuilder.Factory<RelationBuilder> {
        override fun mimeType() = Relation.CONTENT_ITEM_TYPE
        override fun newInstance(dataRowUri: Uri, rawContactId: Long?, contact: Contact, readOnly: Boolean) =
            RelationBuilder(dataRowUri, rawContactId, contact, readOnly)
    }

}
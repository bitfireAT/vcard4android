package at.bitfire.vcard4android.datarow

import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import at.bitfire.vcard4android.BatchOperation
import at.bitfire.vcard4android.Contact
import ezvcard.parameter.AddressType
import java.util.*

class StructuredPostalBuilder(mimeType: String, dataRowUri: Uri, rawContactId: Long?, contact: Contact)
    : DataRowBuilder(mimeType, dataRowUri, rawContactId, contact) {

    override fun build(): List<BatchOperation.CpoBuilder> {
        val result = LinkedList<BatchOperation.CpoBuilder>()
        for (labeledAddress in contact.addresses) {
            val address = labeledAddress.property

            var formattedAddress = address.label
            if (formattedAddress.isNullOrBlank()) {
                /*	StructuredPostal.FORMATTED_ADDRESS must not be empty, but no formatted address
                 *  was in the vCard. So we build it like this:
                 *
                 *  street po.box (extended)
                 *	postcode city
                 *	region
                 *	COUNTRY
                 */

                val lineStreet = arrayOf(address.streetAddress, address.poBox, address.extendedAddress).filterNot { it.isNullOrEmpty() }.joinToString(" ")
                val lineLocality = arrayOf(address.postalCode, address.locality).filterNot { it.isNullOrEmpty() }.joinToString(" ")

                val lines = LinkedList<String>()
                if (lineStreet.isNotEmpty())
                    lines += lineStreet
                if (lineLocality.isNotEmpty())
                    lines += lineLocality
                if (!address.region.isNullOrEmpty())
                    lines += address.region
                if (!address.country.isNullOrEmpty())
                    lines += address.country.toUpperCase(Locale.getDefault())

                formattedAddress = lines.joinToString("\n")
            }

            val types = address.types
            val typeCode: Int
            var typeLabel: String? = null
            if (labeledAddress.label != null) {
                typeCode = StructuredPostal.TYPE_CUSTOM
                typeLabel = labeledAddress.label
            } else
                typeCode = when {
                    types.contains(AddressType.HOME) -> StructuredPostal.TYPE_HOME
                    types.contains(AddressType.WORK) -> StructuredPostal.TYPE_WORK
                    else -> StructuredPostal.TYPE_OTHER
                }

            val builder = newDataRow()
                    .withValue(StructuredPostal.FORMATTED_ADDRESS, formattedAddress)
                    .withValue(StructuredPostal.TYPE, typeCode)
                    .withValue(StructuredPostal.LABEL, typeLabel)
                    .withValue(StructuredPostal.STREET, address.streetAddress)
                    .withValue(StructuredPostal.POBOX, address.poBox)
                    .withValue(StructuredPostal.NEIGHBORHOOD, address.extendedAddress)
                    .withValue(StructuredPostal.CITY, address.locality)
                    .withValue(StructuredPostal.REGION, address.region)
                    .withValue(StructuredPostal.POSTCODE, address.postalCode)
                    .withValue(StructuredPostal.COUNTRY, address.country)
        }
        return result
    }


    object Factory: DataRowBuilder.Factory<StructuredPostalBuilder> {
        override fun mimeType() = StructuredPostal.CONTENT_ITEM_TYPE
        override fun newInstance(dataRowUri: Uri, rawContactId: Long?, contact: Contact) =
            StructuredPostalBuilder(mimeType(), dataRowUri, rawContactId, contact)
    }

}
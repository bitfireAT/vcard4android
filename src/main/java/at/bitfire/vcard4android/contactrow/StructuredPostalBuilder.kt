package at.bitfire.vcard4android.contactrow

import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import at.bitfire.vcard4android.BatchOperation
import at.bitfire.vcard4android.Contact
import ezvcard.parameter.AddressType
import org.apache.commons.lang3.StringUtils
import java.util.*

/**
 * Data row builder for structured addresses.
 *
 * Android requires a formatted address. If the contact data doesn't contain a
 * formatted address, it's built like this:
 *
 *     | field             | example                      |
 *     |-------------------|------------------------------|
 *     | street            | Sample Street 123            |
 *     | poBox             | P/O Box 45                   |
 *     | extended          | Near the park                |
 *     | postalCode city   | 12345 Sampletown             |
 *     | country (region)  | Samplecountry (Sampleregion) |
 *
 * TODO: should be localized (there are many different international formats)
 *
 */
class StructuredPostalBuilder(dataRowUri: Uri, rawContactId: Long?, contact: Contact)
    : DataRowBuilder(Factory.mimeType(), dataRowUri, rawContactId, contact) {

    override fun build(): List<BatchOperation.CpoBuilder> {
        val result = LinkedList<BatchOperation.CpoBuilder>()
        for (labeledAddress in contact.addresses) {
            val address = labeledAddress.property

            var formattedAddress = address.label
            if (formattedAddress.isNullOrBlank()) {
                val lines = LinkedList<String>()
                for (street in address.streetAddresses.filterNot { s -> s.isNullOrBlank() })
                    lines += street
                for (poBox in address.poBoxes.filterNot { s -> s.isNullOrBlank() })
                    lines += poBox
                for (extended in address.extendedAddresses.filterNot { s -> s.isNullOrBlank() })
                    lines += extended

                val postalAndCity = LinkedList<String>()
                if (StringUtils.trimToNull(address.postalCode) != null)
                    postalAndCity += address.postalCodes.joinToString(" / ")
                if (StringUtils.trimToNull(address.locality) != null)
                    postalAndCity += address.localities.joinToString(" / ")
                if (postalAndCity.isNotEmpty())
                    lines += postalAndCity.joinToString(" ")

                if (StringUtils.trimToNull(address.country) != null) {
                    val line = StringBuilder(address.countries.joinToString(" / "))
                    if (!address.region.isNullOrBlank()) {
                        val regions = address.regions.joinToString(" / ")
                        line.append(" ($regions)")
                    }
                    lines += line.toString()
                } else
                    if (!address.region.isNullOrBlank())
                        lines += address.regions.joinToString(" / ")

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

            result += newDataRow()
                    .withValue(StructuredPostal.FORMATTED_ADDRESS, formattedAddress)
                    .withValue(StructuredPostal.TYPE, typeCode)
                    .withValue(StructuredPostal.LABEL, typeLabel)
                    .withValue(StructuredPostal.STREET, address.streetAddresses.joinToString("\n"))
                    .withValue(StructuredPostal.POBOX, address.poBoxes.joinToString("\n"))
                    .withValue(StructuredPostal.NEIGHBORHOOD, address.extendedAddresses.joinToString("\n"))
                    .withValue(StructuredPostal.CITY, address.localities.joinToString("\n"))
                    .withValue(StructuredPostal.REGION, address.regions.joinToString("\n"))
                    .withValue(StructuredPostal.POSTCODE, address.postalCodes.joinToString("\n"))
                    .withValue(StructuredPostal.COUNTRY, address.countries.joinToString("\n"))
        }
        return result
    }


    object Factory: DataRowBuilder.Factory<StructuredPostalBuilder> {
        override fun mimeType() = StructuredPostal.CONTENT_ITEM_TYPE
        override fun newInstance(dataRowUri: Uri, rawContactId: Long?, contact: Contact) =
            StructuredPostalBuilder(dataRowUri, rawContactId, contact)
    }

}
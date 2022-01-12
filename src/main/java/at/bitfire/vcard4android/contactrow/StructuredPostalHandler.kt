/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.content.ContentValues
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import at.bitfire.vcard4android.Contact
import at.bitfire.vcard4android.LabeledProperty
import ezvcard.parameter.AddressType
import ezvcard.property.Address

object StructuredPostalHandler: DataRowHandler() {

    override fun forMimeType() = StructuredPostal.CONTENT_ITEM_TYPE

    override fun handle(values: ContentValues, contact: Contact) {
        super.handle(values, contact)

        val address = Address()
        val labeledAddress = LabeledProperty(address)

        address.label = values.getAsString(StructuredPostal.FORMATTED_ADDRESS)
        when (values.getAsInteger(StructuredPostal.TYPE)) {
            StructuredPostal.TYPE_HOME ->
                address.types += AddressType.HOME
            StructuredPostal.TYPE_WORK ->
                address.types += AddressType.WORK
            StructuredPostal.TYPE_CUSTOM -> {
                values.getAsString(StructuredPostal.LABEL)?.let {
                    labeledAddress.label = it
                }
            }
        }
        values.getAsString(StructuredPostal.STREET)?.let { streets ->
            address.streetAddresses += streets.split('\n')
        }
        values.getAsString(StructuredPostal.POBOX)?.let { poBoxes ->
            address.poBoxes += poBoxes.split('\n')
        }
        values.getAsString(StructuredPostal.NEIGHBORHOOD)?.let { neighborhoods ->
            address.extendedAddresses += neighborhoods.split('\n')
        }
        values.getAsString(StructuredPostal.CITY)?.let { cities ->
            address.localities += cities.split('\n')
        }
        values.getAsString(StructuredPostal.REGION)?.let { regions ->
            address.regions += regions.split('\n')
        }
        values.getAsString(StructuredPostal.POSTCODE)?.let { postalCodes ->
            address.postalCodes += postalCodes.split('\n')
        }
        values.getAsString(StructuredPostal.COUNTRY)?.let { countries ->
            address.countries += countries.split('\n')
        }

        if (address.streetAddresses.isNotEmpty() ||
            address.poBoxes.isNotEmpty() ||
            address.extendedAddresses.isNotEmpty() ||
            address.localities.isNotEmpty() ||
            address.regions.isNotEmpty() ||
            address.postalCodes.isNotEmpty() ||
            address.countries.isNotEmpty())
            contact.addresses += labeledAddress
    }

}
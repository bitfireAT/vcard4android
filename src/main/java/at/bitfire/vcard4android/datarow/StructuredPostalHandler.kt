package at.bitfire.vcard4android.datarow

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
        address.streetAddress = values.getAsString(StructuredPostal.STREET)
        address.poBox = values.getAsString(StructuredPostal.POBOX)
        address.extendedAddress = values.getAsString(StructuredPostal.NEIGHBORHOOD)
        address.locality = values.getAsString(StructuredPostal.CITY)
        address.region = values.getAsString(StructuredPostal.REGION)
        address.postalCode = values.getAsString(StructuredPostal.POSTCODE)
        address.country = values.getAsString(StructuredPostal.COUNTRY)
        contact.addresses += labeledAddress
    }

}
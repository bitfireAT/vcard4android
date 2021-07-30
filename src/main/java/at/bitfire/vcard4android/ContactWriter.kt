package at.bitfire.vcard4android

import at.bitfire.vcard4android.property.*
import ezvcard.Ezvcard
import ezvcard.VCard
import ezvcard.VCardVersion
import ezvcard.io.text.VCardWriter
import ezvcard.parameter.ImageType
import ezvcard.property.*
import org.apache.commons.lang3.StringUtils
import java.io.OutputStream
import java.util.*
import java.util.logging.Level

/**
 * Responsible for converting the [Contact] data class (which is not version-specific)
 * to the vCard that is actually sent to the server.
 *
 * Properties which are not supported by the target vCard version have to be converted appropriately.
 */
class ContactWriter private constructor(val contact: Contact, val version: VCardVersion, val groupMethod: GroupMethod) {

    private val unknownProperties = LinkedList<VCardProperty>()
    val vCard = VCard()

    /** counter for item ID of labelled properties: 1 means "item1." etc. */
    private var currentItemId = 1

    companion object {

        fun fromContact(contact: Contact, version: VCardVersion, groupMethod: GroupMethod) =
            ContactWriter(contact, version, groupMethod)

    }

    init {
        parseUnknownProperties()
        addProperties()
    }

    fun addProperties() {
        contact.uid?.let { vCard.uid = Uid(it) }
        Contact.productID?.let { vCard.setProductId(it) }

        addKindAndMembers()

        addFormattedName()
        addStructuredName()
        addPhoneticName()
        contact.nickName?.let { nickName -> addLabeledProperty(nickName) }

        if (contact.categories.isNotEmpty())
            vCard.addCategories(Categories().apply {
                values.addAll(contact.categories)
            })

        addOrganization()

        for (phone in contact.phoneNumbers)
            addLabeledProperty(phone)
        for (email in contact.emails)
            addLabeledProperty(email)
        for (impp in contact.impps)
            addLabeledProperty(impp)
        for (url in contact.urls)
            addLabeledProperty(url)

        for (address in contact.addresses)
            addLabeledProperty(address)

        addDates()

        for (relation in contact.relations)
            vCard.addRelated(relation)

        contact.note?.let { note -> vCard.addNote(note) }

        for (unknownProperty in unknownProperties)
            vCard.addProperty(unknownProperty)

        contact.photo?.let { photo -> vCard.addPhoto(Photo(photo, ImageType.JPEG)) }

        vCard.revision = Revision.now()
    }

    private fun addDates() {
        contact.birthDay?.let { birthday ->
            // vCard3 doesn't support partial dates
            if (version == VCardVersion.V3_0)
                rewritePartialDate(birthday)

            vCard.birthday = birthday
        }

        contact.anniversary?.let { anniversary ->
            if (version == VCardVersion.V4_0) {
                vCard.anniversary = anniversary

            } else /* version == VCardVersion.V3_0 */ {
                // vCard3 doesn't support partial dates
                rewritePartialDate(anniversary)
                // vCard3 doesn't support ANNIVERSARY, rewrite to X-ABDate
                addLabeledProperty(LabeledProperty(XAbDate(anniversary.date), Contact.DATE_LABEL_ANNIVERSARY))
                vCard.anniversary = null
            }
        }

        for (customDate in contact.customDates) {
            rewritePartialDate(customDate.property)
            addLabeledProperty(customDate)
        }
    }

    private fun addFormattedName() {
        if (version == VCardVersion.V4_0) {
            contact.displayName?.let { fn -> vCard.setFormattedName(fn) }

        } else /* version == VCardVersion.V3_0 */ {
            // vCard 3 REQUIRES FN [RFC 2426 p. 29]
            var fn =
                    // use display name, if available
                    StringUtils.trimToNull(contact.displayName) ?:
                    // no display name, try organization
                    contact.organization?.let { org ->
                        org.values.joinToString(" / ")
                    } ?:
                    // otherwise, try nickname
                    contact.nickName?.let { nick -> nick.property.values.firstOrNull() } ?:
                    // otherwise, try email address
                    contact.emails.firstOrNull()?.let { email -> email.property.value } ?:
                    // otherwise, try phone number
                    contact.phoneNumbers.firstOrNull()?.let { phone -> phone.property.text } ?:
                    // otherwise, try UID or use empty string
                    contact.uid ?: ""
            vCard.setFormattedName(fn)
        }
    }

    private fun addKindAndMembers() {
        if (contact.group && groupMethod == GroupMethod.GROUP_VCARDS) {
            // TODO Use urn:uuid only when applicable
            if (version == VCardVersion.V4_0) {         // vCard4
                vCard.kind = Kind.group()
                for (member in contact.members)
                    vCard.addMember(Member("urn:uuid:$member"))
            } else {                                    // "vCard4 as vCard3" (Apple-style)
                vCard.setProperty(XAddressBookServerKind(Kind.GROUP))
                for (member in contact.members)
                    vCard.addProperty(XAddressBookServerMember("urn:uuid:$member"))
            }
        }
    }

    private fun addOrganization() {
        contact.organization?.let { vCard.organization = it }
        contact.jobTitle?.let { vCard.addTitle(it) }
        contact.jobDescription?.let { vCard.addRole(it) }
    }

    private fun addStructuredName() {
        val n = StructuredName()

        contact.prefix?.let { prefixesStr ->
            for (prefix in prefixesStr.split(' '))
                n.prefixes += prefix
        }

        n.given = contact.givenName
        contact.middleName?.let { middleNamesStr ->
            for (middleName in middleNamesStr.split(' '))
                n.additionalNames += middleName
        }
        n.family = contact.familyName

        contact.suffix?.let { suffixesStr ->
            for (suffix in suffixesStr.split(' '))
                n.suffixes += suffix
        }

        if (version == VCardVersion.V4_0) {
            // add N only if there's some data in it
            if (n.prefixes.isNotEmpty() || n.given != null || n.additionalNames.isNotEmpty() || n.family != null || n.suffixes.isNotEmpty())
                vCard.structuredName = n

        } else /* version == VCardVersion.V3_0 */ {
            // vCard 3 REQUIRES N [RFC 2426 p. 29]
            vCard.structuredName = n
        }
    }

    private fun addPhoneticName() {
        contact.phoneticGivenName?.let { firstName ->
            vCard.addProperty(XPhoneticFirstName(firstName))
        }
        contact.phoneticMiddleName?.let { middleName ->
            vCard.addProperty(XPhoneticMiddleName(middleName))
        }
        contact.phoneticFamilyName?.let { lastName ->
            vCard.addProperty(XPhoneticLastName(lastName))
        }
    }


    // helpers

    fun addLabeledProperty(labeledProperty: LabeledProperty<*>) {
        val property = labeledProperty.property

        if (labeledProperty.label != null) {
            // property with label -> group property and label with item ID
            val itemId = getNextItemId()

            // 1. add property with item ID
            property.group = itemId

            // 2. add label with same item ID
            val label = XAbLabel(labeledProperty.label)
            label.group = itemId
            vCard.addProperty(label)
        }

        vCard.addProperty(property)
    }

    private fun getNextItemId(): String {
        var id: String

        // increase ID until there is one which is not already used by an unknown property
        do {
            id = "item${currentItemId++}"
        } while (unknownProperties.any { it.group == id })

        return id
    }

    private fun parseUnknownProperties() {
        try {
            contact.unknownProperties?.let {
                Ezvcard.parse(it).first()?.let { vCard ->
                    unknownProperties.addAll(vCard.properties)
                }
            }
        } catch (e: Exception) {
            Constants.log.log(Level.WARNING, "Couldn't parse original vCard with retained properties", e)
        }
    }

    fun<T: DateOrTimeProperty> rewritePartialDate(prop: T) {
        // vCard 3 doesn't understand partial dates:
        //   1. the syntax is different (vCard 3: 2021-03-07, partial date: 20210307)
        //   2. vCard 3 doesn't understand dates without year
        val partial = prop.partialDate
        if (version == VCardVersion.V3_0 && prop.date == null && partial != null) {
            val originalYear = partial.year
            val year = originalYear ?: Contact.DATE_PARAMETER_OMIT_YEAR_DEFAULT

            // use full date format
            val fakeCal = GregorianCalendar.getInstance()
            fakeCal.timeInMillis = 0    // reset everything, including milliseconds
            fakeCal.set(year, partial.month - 1, partial.date, 0, 0, 0)
            prop.setDate(fakeCal, false)

            if (originalYear == null)
                prop.addParameter(Contact.DATE_PARAMETER_OMIT_YEAR, Contact.DATE_PARAMETER_OMIT_YEAR_DEFAULT.toString())
        }
    }


    fun writeVCard(stream: OutputStream) {
        // validate vCard and log results
        val validation = vCard.validate(version)
        if (!validation.isEmpty) {
            val msgs = LinkedList<String>()
            for ((key, warnings) in validation)
                msgs += "  * " + key?.javaClass?.simpleName + " - " + warnings?.joinToString(" | ")
            Constants.log.log(Level.WARNING, "vCard validation warnings", msgs.joinToString(","))
        }

        val writer = VCardWriter(stream, version).apply {
            isAddProdId = Contact.productID == null
            CustomScribes.registerAt(scribeIndex)

            // include trailing semicolons for maximum compatibility
            isIncludeTrailingSemicolons = true

            // use caret encoding for parameter values (RFC 6868)
            isCaretEncodingEnabled = true

            isVersionStrict = false
        }
        writer.write(vCard)
        writer.flush()
    }

}
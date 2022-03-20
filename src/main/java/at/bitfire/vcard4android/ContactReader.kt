/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android

import at.bitfire.vcard4android.property.*
import at.bitfire.vcard4android.property.CustomScribes.registerCustomScribes
import ezvcard.Ezvcard
import ezvcard.VCard
import ezvcard.parameter.RelatedType
import ezvcard.property.*
import ezvcard.util.PartialDate
import org.apache.commons.lang3.StringUtils
import java.net.URI
import java.net.URISyntaxException
import java.util.*
import java.util.logging.Level

/**
 * Responsible for converting a specific vCard with a specific version to
 * the version-independent data class [Contact].
 *
 * Attention: This class works with the original vCard and modifies it!
 */
class ContactReader internal constructor(val vCard: VCard, val downloader: Contact.Downloader? = null) {

    companion object {

        /**
         * Maximum size for binary data like LOGO and SOUND data URIs.
         *
         * Data larger than this size has unfortunately to be dropped because of Android's IPC limits
         * (max 1 MB per transaction, for all rows together).
         */
        const val MAX_BINARY_DATA_SIZE = 25*1024

        fun fromVCard(vCard: VCard, downloader: Contact.Downloader? = null) =
            ContactReader(vCard, downloader).toContact()

        fun checkPartialDate(prop: DateOrTimeProperty) {
            val date = prop.date
            if (prop.partialDate == null && date != null) {
                prop.getParameter(Contact.DATE_PARAMETER_OMIT_YEAR)?.let { omitYearStr ->
                    val cal = GregorianCalendar.getInstance()
                    cal.time = date
                    if (cal.get(GregorianCalendar.YEAR).toString() == omitYearStr) {
                        val partial = PartialDate.builder()
                            .date(cal.get(GregorianCalendar.DAY_OF_MONTH))
                            .month(cal.get(GregorianCalendar.MONTH) + 1)
                            .build()
                        prop.partialDate = partial
                    }

                    // X-APPLE-OMIT-YEAR not required anymore because we're now working with PartialDate
                    prop.removeParameter(Contact.DATE_PARAMETER_OMIT_YEAR)
                }
            }
        }

        fun uriToUid(uriString: String?): String? {
            if (uriString == null)
                return null

            val uid = try {
                val uri = URI(uriString)
                when {
                    uri.scheme == null ->
                        uri.schemeSpecificPart
                    uri.scheme.equals("urn", true) && uri.schemeSpecificPart.startsWith("uuid:", true) ->
                        uri.schemeSpecificPart.substring(5)
                    else ->
                        uriString
                }
            } catch(e: URISyntaxException) {
                Constants.log.warning("Invalid URI for UID: $uriString")
                uriString
            }

            return StringUtils.trimToNull(uid)
        }

    }


    /**
     * Converts the vCard to a [Contact].
     */
    private fun toContact(): Contact {
        val c = Contact()

        // process standard properties; after processing, only unknown properties will remain
        for (prop in vCard.properties) {
            var remove = true       // assume that this property will be processed and thus shall be removed

            when (prop) {
                is Uid ->
                    c.uid = uriToUid(prop.value)

                is Kind, is XAddressBookServerKind -> {
                    val kindProp = prop as Kind
                    c.group = kindProp.isGroup
                }
                is Member, is XAddressBookServerMember -> {
                    val uriProp = prop as Member
                    uriToUid(uriProp.uri)?.let { c.members += it }
                }

                is FormattedName ->
                    c.displayName = StringUtils.trimToNull(prop.value)
                is StructuredName -> {
                    c.prefix = StringUtils.trimToNull(prop.prefixes.joinToString(" "))
                    c.givenName = StringUtils.trimToNull(prop.given)
                    c.middleName = StringUtils.trimToNull(prop.additionalNames.joinToString(" "))
                    c.familyName = StringUtils.trimToNull(prop.family)
                    c.suffix = StringUtils.trimToNull(prop.suffixes.joinToString(" "))
                }
                is XPhoneticFirstName ->
                    c.phoneticGivenName = StringUtils.trimToNull(prop.value)
                is XPhoneticMiddleName ->
                    c.phoneticMiddleName = StringUtils.trimToNull(prop.value)
                is XPhoneticLastName ->
                    c.phoneticFamilyName = StringUtils.trimToNull(prop.value)
                is Nickname ->
                    c.nickName = LabeledProperty(prop, findAndRemoveLabel(prop.group))

                is Categories ->
                    c.categories.addAll(prop.values)

                is Organization ->
                    c.organization = prop
                is Title ->
                    c.jobTitle = StringUtils.trimToNull(prop.value)
                is Role ->
                    c.jobDescription = StringUtils.trimToNull(prop.value)

                is Telephone ->
                    if (!prop.text.isNullOrBlank())
                        c.phoneNumbers += LabeledProperty(prop, findAndRemoveLabel(prop.group))
                is Email ->
                    if (!prop.value.isNullOrBlank())
                        c.emails += LabeledProperty(prop, findAndRemoveLabel(prop.group))
                is Impp ->
                    c.impps += LabeledProperty(prop, findAndRemoveLabel(prop.group))
                is XSip ->
                    // special case: treat  X-SIP:address  as  IMPP:sip:address
                    c.impps += LabeledProperty(Impp.sip(prop.value))
                is Url ->
                    c.urls += LabeledProperty(prop, findAndRemoveLabel(prop.group))

                is Address ->
                    c.addresses += LabeledProperty(prop, findAndRemoveLabel(prop.group))
                is Label -> { /* drop vCard3 formatted address because it can't be associated to a specific address */ }

                is Anniversary -> {
                    checkPartialDate(prop)
                    c.anniversary = prop
                }
                is Birthday -> {
                    checkPartialDate(prop)
                    c.birthDay = prop
                }
                is XAbDate -> {
                    checkPartialDate(prop)
                    var label = findAndRemoveLabel(prop.group)
                    if (label == XAbLabel.APPLE_OTHER)              // drop Apple "Other" label
                        label = null

                    if (label == XAbLabel.APPLE_ANNIVERSARY) {      // convert custom date with Apple "Anniversary" label to real anniversary
                        if (prop.date != null)
                            c.anniversary = Anniversary(prop.date)
                        else if (prop.partialDate != null)
                            c.anniversary = Anniversary(prop.partialDate)
                    } else
                        c.customDates += LabeledProperty(prop, label)
                }

                is Related ->
                    if (!prop.uri.isNullOrBlank() || !prop.text.isNullOrBlank())
                        c.relations += prop
                is XAbRelatedNames -> {
                    val relation = Related()
                    relation.text = prop.value

                    val labelStr = findAndRemoveLabel(prop.group)
                    when (labelStr) {
                        XAbRelatedNames.APPLE_ASSISTANT -> {
                            relation.types.add(CustomType.Related.ASSISTANT)
                            relation.types.add(RelatedType.CO_WORKER)
                        }
                        XAbRelatedNames.APPLE_BROTHER -> {
                            relation.types.add(CustomType.Related.BROTHER)
                            relation.types.add(RelatedType.SIBLING)
                        }
                        XAbRelatedNames.APPLE_CHILD ->
                            relation.types.add(RelatedType.CHILD)
                        XAbRelatedNames.APPLE_FATHER -> {
                            relation.types.add(CustomType.Related.FATHER)
                            relation.types.add(RelatedType.PARENT)
                        }
                        XAbRelatedNames.APPLE_FRIEND ->
                            relation.types.add(RelatedType.FRIEND)
                        XAbRelatedNames.APPLE_MANAGER -> {
                            relation.types.add(CustomType.Related.MANAGER)
                            relation.types.add(RelatedType.CO_WORKER)
                        }
                        XAbRelatedNames.APPLE_MOTHER -> {
                            relation.types.add(CustomType.Related.MOTHER)
                            relation.types.add(RelatedType.PARENT)
                        }
                        XAbRelatedNames.APPLE_SISTER -> {
                            relation.types.add(CustomType.Related.SISTER)
                            relation.types.add(RelatedType.SIBLING)
                        }
                        XAbRelatedNames.APPLE_PARENT ->
                            relation.types.add(RelatedType.PARENT)
                        XAbRelatedNames.APPLE_PARTNER ->
                            relation.types.add(CustomType.Related.PARTNER)
                        XAbRelatedNames.APPLE_SPOUSE ->
                            relation.types.add(RelatedType.SPOUSE)

                        is String /* label != null */ -> {
                            for (label in labelStr.split(','))
                                relation.types.add(RelatedType.get(label.trim().lowercase()))
                        }
                    }
                    c.relations += relation
                }

                is Note -> {
                    StringUtils.trimToNull(prop.value)?.let { note ->
                        if (c.note == null)
                            c.note = note
                        else
                            c.note += "\n\n\n" + note
                    }
                }

                is Photo ->
                    c.photo = c.photo ?: getPhotoBytes(prop)

                // drop large binary properties because of potential OutOfMemory / TransactionTooLarge exceptions
                is Logo -> {
                    remove = prop.data != null && prop.data.size > MAX_BINARY_DATA_SIZE
                }
                is Sound -> {
                    remove = prop.data != null && prop.data.size > MAX_BINARY_DATA_SIZE
                }

                // remove properties that don't apply anymore
                is ProductId,
                is Revision,
                is SortString,      // not counterpart in Android; remove it because FN/N may be changed, which would cause inconsistency
                is Source -> {      // when we upload a modified contact, the SOURCE would maybe point to a different version, which would cause inconsistency
                    /* remove = true */
                }

                else ->     // unknown property, keep it in vCard in order to retain it
                    remove = false
            }

            if (remove)
                vCard.removeProperty(prop)
        }

        if (c.uid == null) {
            Constants.log.warning("Received vCard without UID, generating new one")
            c.uid = UUID.randomUUID().toString()
        }

        // remove properties which
        // - couldn't be parsed (and thus are treated as extended/unknown properties), and
        // - must not occur more than once
        arrayOf("ANNIVERSARY", "BDAY", "KIND", "FN", "N", "PRODID", "REV", "UID").forEach {
            vCard.removeExtendedProperty(it)
        }

        if (vCard.properties.isNotEmpty() || vCard.extendedProperties.isNotEmpty())
            try {
                c.unknownProperties = Ezvcard
                        .write(vCard)
                        .prodId(false)
                        .version(vCard.version)
                        .registerCustomScribes()
                        .go()
            } catch(e: Exception) {
                Constants.log.log(Level.WARNING, "Couldn't serialize unknown properties, dropping them", e)
            }

        return c
    }


    // helpers

    fun findAndRemoveLabel(group: String?): String? {
        if (group == null)
            return null

        for (label in vCard.getProperties(XAbLabel::class.java)) {
            if (label.group.equals(group, true)) {
                vCard.removeProperty(label)
                return StringUtils.trimToNull(label.value)
            }
        }

        return null
    }

    fun getPhotoBytes(photo: Photo): ByteArray? {
        if (photo.data != null)
            return photo.data

        val url = photo.url
        if (photo.url != null && downloader != null) {
            Constants.log.info("Downloading photo from $url")
            return downloader.download(url, "image/*")
        }

        return null
    }

}
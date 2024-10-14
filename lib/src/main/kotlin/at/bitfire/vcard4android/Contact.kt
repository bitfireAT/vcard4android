/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android

import at.bitfire.vcard4android.property.CustomScribes.registerCustomScribes
import at.bitfire.vcard4android.property.XAbDate
import com.google.common.base.Ascii
import com.google.common.base.MoreObjects
import ezvcard.VCardVersion
import ezvcard.io.json.JCardReader
import ezvcard.io.text.VCardReader
import ezvcard.property.*
import java.io.IOException
import java.io.OutputStream
import java.io.Reader
import java.util.*

/**
 * Data class for a contact; between vCards and the Android contacts provider.
 *
 * Data shall be stored without workarounds in the most appropriate form. For instance,
 * an anniversary should be stored as [anniversary] and not in [customDates] with a
 * proprietary label. Strings should not be empty values (rather *null* in this case).
 *
 * vCards are parsed to [Contact]s by [ContactReader].
 * [Contact]s are written to vCards by [ContactWriter].
 *
 * [Contact]s are written to and read from the Android storage by [AndroidContact].
 */
data class Contact(
    var uid: String? = null,
    var group: Boolean = false,

    /** list of UIDs of group members without urn:uuid prefix (only meaningful if [group] is true) */
    val members: MutableSet<String> = mutableSetOf(),

    var displayName: String? = null,
    var prefix: String? = null,
    var givenName: String? = null,
    var middleName: String? = null,
    var familyName: String? = null,
    var suffix: String? = null,

    var phoneticGivenName: String? = null,
    var phoneticMiddleName: String? = null,
    var phoneticFamilyName: String? = null,

    /** vCard NICKNAME – Android only supports one nickname **/
    var nickName: LabeledProperty<Nickname>? = null,

    var organization: Organization? = null,
    var jobTitle: String? = null,           // vCard TITLE
    var jobDescription: String? = null,     // vCard ROLE

    val phoneNumbers: LinkedList<LabeledProperty<Telephone>> = LinkedList(),
    val emails: LinkedList<LabeledProperty<Email>> = LinkedList(),
    val impps: LinkedList<LabeledProperty<Impp>> = LinkedList(),
    val addresses: LinkedList<LabeledProperty<Address>> = LinkedList(),
    val categories: LinkedList<String> = LinkedList(),
    val urls: LinkedList<LabeledProperty<Url>> = LinkedList(),
    val relations: LinkedList<Related> = LinkedList(),

    var note: String? = null,

    var anniversary: Anniversary? = null,
    var birthDay: Birthday? = null,
    val customDates: LinkedList<LabeledProperty<XAbDate>> = LinkedList(),

    var photo: ByteArray? = null,

    /** unknown properties in text vCard format */
    var unknownProperties: String? = null
) {

    companion object {
        // productID (if set) will be used to generate a PRODID property.
        // You may set this statically from the calling application.
        var productID: String? = null

        const val DATE_PARAMETER_OMIT_YEAR = "X-APPLE-OMIT-YEAR"
        const val DATE_PARAMETER_OMIT_YEAR_DEFAULT = 1604

        const val TO_STRING_MAX_VALUE_SIZE = 2000

        /**
         * Parses an InputStream that contains a vCard.
         *
         * @param reader     reader for the input stream containing the vCard (pay attention to the charset)
         * @param downloader will be used to download external resources like contact photos (may be null)
         * @param jCard      *true*: content is jCard; *false*: content is vCard
         *
         * @return list of filled Event data objects (may have size 0) – doesn't return null
         *
         * @throws IOException on I/O errors when reading the stream
         * @throws ezvcard.io.CannotParseException when the vCard can't be parsed
         */
        fun fromReader(reader: Reader, jCard: Boolean, downloader: Downloader?): List<Contact>  {
            // create new reader and add custom scribes
            val vCards =
                if (jCard)
                    JCardReader(reader)
                        .registerCustomScribes()
                        .readAll()
                else
                    VCardReader(reader, VCardVersion.V3_0)        // CardDAV requires vCard 3 or newer
                        .registerCustomScribes()
                        .readAll()

            return vCards.map { vCard ->
                // convert every vCard to a Contact data object
                ContactReader.fromVCard(vCard, downloader)
            }
        }

    }


    @Throws(IOException::class)
    fun writeJCard(os: OutputStream) {
        val generator = ContactWriter.fromContact(this, VCardVersion.V4_0)
        generator.writeCard(os, true)
    }

    @Throws(IOException::class)
    fun writeVCard(vCardVersion: VCardVersion, os: OutputStream) {
        val generator = ContactWriter.fromContact(this, vCardVersion)
        generator.writeCard(os, false)
    }


    private fun compareFields(): Array<Any?> = arrayOf(
        uid,
        group,
        members,
        displayName, prefix, givenName, middleName, familyName, suffix,
        phoneticGivenName, phoneticMiddleName, phoneticFamilyName,
        nickName,
        organization, jobTitle, jobDescription,
        phoneNumbers, emails, impps, addresses, /* categories, */ urls, relations,
        note, anniversary, birthDay, customDates,
        photo
        /* unknownProperties */
    )

    override fun equals(other: Any?) =
        if (other is Contact)
            compareFields().contentDeepEquals(other.compareFields())
        else
            false

    override fun hashCode() = compareFields().contentHashCode()

    /**
     * Implements [Object.toString]. Truncates properties with potential big values:
     *
     * - [photo]
     * - [unknownProperties]
     */
    override fun toString() = MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("uid", uid)
        .add("group", group)
        .add("members", members)
        .add("displayName", displayName)
        .add("prefix", prefix)
        .add("givenName", givenName)
        .add("middleName", middleName)
        .add("familyName", familyName)
        .add("suffix", suffix)
        .add("phoneticGivenName", phoneticGivenName)
        .add("phoneticMiddleName", phoneticMiddleName)
        .add("phoneticFamilyName", phoneticFamilyName)
        .add("nickName", nickName)
        .add("organization", organization)
        .add("jobTitle", jobTitle)
        .add("jobDescription", jobDescription)
        .add("phoneNumbers", phoneNumbers)
        .add("emails", emails)
        .add("impps", impps)
        .add("addresses", addresses)
        .add("categories", categories)
        .add("urls", urls)
        .add("relations", relations)
        .add("note", note?.let { Ascii.truncate(it, TO_STRING_MAX_VALUE_SIZE, "...") })
        .add("anniversary", anniversary)
        .add("birthDay", birthDay)
        .add("customDates", customDates)
        .add("photo", photo?.let { "byte[${it.size}]" })
        .add("unknownProperties", unknownProperties?.let { Ascii.truncate(it, TO_STRING_MAX_VALUE_SIZE, "...") })
        .toString()


    interface Downloader {
        fun download(url: String, accepts: String): ByteArray?
    }

}
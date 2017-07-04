/*
 * Copyright © Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.vcard4android;

import android.annotation.SuppressLint
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.content.EntityIterator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.RemoteException
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds
import android.provider.ContactsContract.CommonDataKinds.*
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Nickname
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Photo
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.RawContacts
import ezvcard.parameter.*
import ezvcard.property.*
import ezvcard.util.PartialDate
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.text.WordUtils
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Level

open class AndroidContact(
        val addressBook: AndroidAddressBook<AndroidContact, AndroidGroup>
) {

    companion object {

        @JvmField val COLUMN_FILENAME = RawContacts.SOURCE_ID
        @JvmField val COLUMN_UID = RawContacts.SYNC1
        @JvmField val COLUMN_ETAG = RawContacts.SYNC2

        @JvmStatic
        protected fun labelToXName(label: String) = "X-" + label
                .replace(" ","_")
                .replace(Regex("[^\\p{L}\\p{Nd}\\-_]"), "")     // TODO check regex
                .toUpperCase()

        @JvmStatic
        protected fun xNameToLabel(xname: String): String {
            // "X-MY_PROPERTY"
            var s = xname.toLowerCase()     // 1. ensure lower case -> "x-my_property"
            if (s.startsWith("x-"))         // 2. remove x- from beginning -> "my_property"
                s = s.substring(2)
            s = s.replace('_', ' ')         // 3. replace "_" by " " -> "my property"
            return WordUtils.capitalize(s)  // 4. capitalize -> "My Property"
        }

        @JvmStatic
        protected fun toURIScheme(s: String) =
                // RFC 3986 3.1
                // scheme      = ALPHA *( ALPHA / DIGIT / "+" / "-" / "." )
                // ALPHA       =  %x41-5A / %x61-7A   ; A-Z / a-z
                // DIGIT       =  %x30-39             ; 0-9
                s.replace(Regex("^[^a-zA-Z]+"), "").replace(Regex("[^\\da-zA-Z+-.]"), "")

    }

    var id: Long? = null
    var fileName: String? = null
    var eTag: String? = null

    val photoMaxDimensions: Int by lazy { queryPhotoMaxDimensions() }


    constructor(addressBook: AndroidAddressBook<AndroidContact, AndroidGroup>, id: Long, fileName: String?, eTag: String?): this(addressBook) {
        this.id = id
        this.fileName = fileName
        this.eTag = eTag
    }

    constructor(addressBook: AndroidAddressBook<AndroidContact, AndroidGroup>, contact: Contact, fileName: String?, eTag: String?): this(addressBook) {
        this.contact = contact
        this.fileName = fileName
        this.eTag = eTag
    }

    var contact: Contact? = null
    @Throws(FileNotFoundException::class, ContactsStorageException::class)
    get() {
        field?.let { return field }

        val id = requireNotNull(id)
        var iter: EntityIterator? = null
        try {
            @SuppressLint("Recycle")
            iter = RawContacts.newEntityIterator(addressBook.provider.query(
                    addressBook.syncAdapterURI(ContactsContract.RawContactsEntity.CONTENT_URI),
                    null, ContactsContract.RawContacts._ID + "=?", arrayOf(id.toString()), null))

            if (iter.hasNext()) {
                val e = iter.next()

                field = Contact()
                populateContact(e.entityValues)

                val subValues = e.subValues
                for (subValue in subValues) {
                    val values = subValue.values

                    // remove empty values
                    val it = values.keySet().iterator()
                    while (it.hasNext()) {
                        val obj = values[it.next()]
                        if (obj is String && obj.isEmpty())
                            it.remove()
                    }
                    
                    val mimeType = values.getAsString(ContactsContract.RawContactsEntity.MIMETYPE)
                    when (mimeType) {
                        StructuredName.CONTENT_ITEM_TYPE ->
                            populateStructuredName(values)
                        Phone.CONTENT_ITEM_TYPE ->
                            populatePhoneNumber(values)
                        Email.CONTENT_ITEM_TYPE ->
                            populateEmail(values)
                        Photo.CONTENT_ITEM_TYPE ->
                            populatePhoto(values)
                        Organization.CONTENT_ITEM_TYPE ->
                            populateOrganization(values)
                        Im.CONTENT_ITEM_TYPE ->
                            populateIMPP(values)
                        Nickname.CONTENT_ITEM_TYPE ->
                            populateNickname(values)
                        Note.CONTENT_ITEM_TYPE ->
                            populateNote(values)
                        StructuredPostal.CONTENT_ITEM_TYPE ->
                            populateStructuredPostal(values)
                        Website.CONTENT_ITEM_TYPE ->
                            populateWebsite(values)
                        Event.CONTENT_ITEM_TYPE ->
                            populateEvent(values)
                        Relation.CONTENT_ITEM_TYPE ->
                            populateRelation(values)
                        SipAddress.CONTENT_ITEM_TYPE ->
                            populateSipAddress(values)
                        null ->
                            Constants.log.warning("Ignoring raw contact data row without ${ContactsContract.RawContactsEntity.MIMETYPE}")
                        else ->
                            populateData(mimeType, values)
                    }
                }

                return field
            } else
                throw FileNotFoundException()
        } catch(e: RemoteException) {
            throw ContactsStorageException("Couldn't read local contact", e)
        } finally {
            iter?.close()
        }
    }

    protected fun populateContact(row: ContentValues) {
        fileName = row.getAsString(COLUMN_FILENAME)
        eTag = row.getAsString(COLUMN_ETAG)

        contact!!.uid = row.getAsString(COLUMN_UID)
    }

    protected fun populateStructuredName(row: ContentValues) {
        val contact = requireNotNull(contact)
        contact.displayName = row.getAsString(StructuredName.DISPLAY_NAME)

        contact.prefix = row.getAsString(StructuredName.PREFIX)
        contact.givenName = row.getAsString(StructuredName.GIVEN_NAME)
        contact.middleName = row.getAsString(StructuredName.MIDDLE_NAME)
        contact.familyName = row.getAsString(StructuredName.FAMILY_NAME)
        contact.suffix = row.getAsString(StructuredName.SUFFIX)

        contact.phoneticGivenName = row.getAsString(StructuredName.PHONETIC_GIVEN_NAME)
        contact.phoneticMiddleName = row.getAsString(StructuredName.PHONETIC_MIDDLE_NAME)
        contact.phoneticFamilyName = row.getAsString(StructuredName.PHONETIC_FAMILY_NAME)
    }

    protected fun populatePhoneNumber(row: ContentValues) {
        val number = Telephone(row.getAsString(Phone.NUMBER))
        val labeledNumber = LabeledProperty(number)

        when (row.getAsInteger(Phone.TYPE)) {
            Phone.TYPE_HOME ->
                number.types += TelephoneType.HOME
            Phone.TYPE_MOBILE ->
                number.types += TelephoneType.CELL
            Phone.TYPE_WORK ->
                number.types += TelephoneType.WORK
            Phone.TYPE_FAX_WORK -> {
                number.types += TelephoneType.FAX
                number.types += TelephoneType.WORK
            }
            Phone.TYPE_FAX_HOME -> {
                number.types += TelephoneType.FAX
                number.types += TelephoneType.HOME
            }
            Phone.TYPE_PAGER ->
                number.types += TelephoneType.PAGER
            Phone.TYPE_CALLBACK ->
                number.types += Contact.PHONE_TYPE_CALLBACK
            Phone.TYPE_CAR ->
                number.types += TelephoneType.CAR
            Phone.TYPE_COMPANY_MAIN ->
                number.types += Contact.PHONE_TYPE_COMPANY_MAIN
            Phone.TYPE_ISDN ->
                number.types += TelephoneType.ISDN
            Phone.TYPE_MAIN ->
                number.types += TelephoneType.VOICE
            Phone.TYPE_OTHER_FAX ->
                number.types += TelephoneType.FAX
            Phone.TYPE_RADIO ->
                number.types += Contact.PHONE_TYPE_RADIO
            Phone.TYPE_TELEX ->
                number.types += TelephoneType.TEXTPHONE
            Phone.TYPE_TTY_TDD ->
                number.types += TelephoneType.TEXT
            Phone.TYPE_WORK_MOBILE -> {
                number.types += TelephoneType.CELL
                number.types += TelephoneType.WORK
            }
            Phone.TYPE_WORK_PAGER -> {
                number.types += TelephoneType.PAGER
                number.types += TelephoneType.WORK
            }
            Phone.TYPE_ASSISTANT ->
                number.types += Contact.PHONE_TYPE_ASSISTANT
            Phone.TYPE_MMS ->
                number.types += Contact.PHONE_TYPE_MMS
            Phone.TYPE_CUSTOM -> {
                    row.getAsString(CommonDataKinds.Phone.LABEL)?.let {
                        labeledNumber.label = it
                        number.types += TelephoneType.get(labelToXName(it))
                    }
                }
        }
        if (row.getAsInteger(CommonDataKinds.Phone.IS_PRIMARY) != 0)
            number.pref = 1

        contact!!.phoneNumbers += labeledNumber
    }

    protected fun populateEmail(row: ContentValues) {
        val email = ezvcard.property.Email(row.getAsString(Email.ADDRESS))
        val labeledEmail = LabeledProperty(email)

        when (row.getAsInteger(Email.TYPE)) {
            Email.TYPE_HOME ->
                email.types += EmailType.HOME
            Email.TYPE_WORK ->
                email.types += EmailType.WORK
            Email.TYPE_MOBILE ->
                email.types += Contact.EMAIL_TYPE_MOBILE
            Email.TYPE_CUSTOM ->
                row.getAsString(Email.LABEL)?.let {
                    labeledEmail.label = it
                    email.types += EmailType.get(labelToXName(it))
                }
        }
        if (row.getAsInteger(Email.IS_PRIMARY) != 0)
            email.pref = 1

        contact!!.emails += labeledEmail
    }

    @Throws(RemoteException::class)
    protected fun populatePhoto(row: ContentValues) {
        val contact = requireNotNull(contact)
        if (row.containsKey(Photo.PHOTO_FILE_ID)) {
            val photoUri = Uri.withAppendedPath(
                    rawContactSyncURI(),
                    RawContacts.DisplayPhoto.CONTENT_DIRECTORY)
            try {
                addressBook.provider.openAssetFile(photoUri, "r")?.use { fd ->
                    fd.createInputStream()?.use { contact.photo = IOUtils.toByteArray(it) }
                }
            } catch(e: IOException) {
                Constants.log.log(Level.WARNING, "Couldn't read local contact photo file", e)
            }
        } else
            contact.photo = row.getAsByteArray(Photo.PHOTO)
    }

    protected fun populateOrganization(row: ContentValues) {
        val contact = requireNotNull(contact)
        
        val company = row.getAsString(Organization.COMPANY)
        val department = row.getAsString(Organization.DEPARTMENT)
        if (company != null || department != null) {
            val org = ezvcard.property.Organization()
            company?.let { org.values += it }
            department?.let { org.values += it }
            contact.organization = org
        }

        row.getAsString(Organization.TITLE)?.let { contact.jobTitle = it }
        row.getAsString(Organization.JOB_DESCRIPTION)?.let { contact.jobDescription = it }
    }

    protected fun populateIMPP(row: ContentValues) {
        val handle = row.getAsString(Im.DATA)
        if (handle == null) {
            Constants.log.warning("Ignoring instant messenger record without handle")
            return
        }

        var impp: Impp? = null
        when (row.getAsInteger(Im.PROTOCOL)) {
            Im.PROTOCOL_AIM ->
                impp = Impp.aim(handle)
            Im.PROTOCOL_MSN ->
                impp = Impp.msn(handle)
            Im.PROTOCOL_YAHOO ->
                impp = Impp.yahoo(handle)
            Im.PROTOCOL_SKYPE ->
                impp = Impp.skype(handle)
            Im.PROTOCOL_QQ ->
                impp = Impp("qq", handle)
            Im.PROTOCOL_GOOGLE_TALK ->
                impp = Impp("google-talk", handle)
            Im.PROTOCOL_ICQ ->
                impp = Impp.icq(handle)
            Im.PROTOCOL_JABBER ->
                impp = Impp.xmpp(handle)
            Im.PROTOCOL_NETMEETING ->
                impp = Impp("netmeeting", handle)
            Im.PROTOCOL_CUSTOM ->
                try {
                    impp = Impp(toURIScheme(row.getAsString(Im.CUSTOM_PROTOCOL)), handle)
                } catch(e: IllegalArgumentException) {
                    Constants.log.warning("Messenger type/value can't be expressed as URI; ignoring")
                }
        }

        impp?.let { impp ->
            val labeledImpp = LabeledProperty(impp)

            when (row.getAsInteger(Im.TYPE)) {
                Im.TYPE_HOME ->
                    impp.types += ImppType.HOME
                Im.TYPE_WORK ->
                    impp.types += ImppType.WORK
                Im.TYPE_CUSTOM ->
                    row.getAsString(Im.LABEL)?.let {
                        labeledImpp.label = it
                        impp.types.add(ImppType.get(labelToXName(it)))
                    }
            }

            contact!!.impps += labeledImpp
        }
    }

    protected fun populateNickname(row: ContentValues) {
        row.getAsString(Nickname.NAME)?.let { name ->
            val nick = ezvcard.property.Nickname()
            nick.values += name

            when (row.getAsInteger(Nickname.TYPE)) {
                Nickname.TYPE_MAIDEN_NAME ->
                    nick.type = Contact.NICKNAME_TYPE_MAIDEN_NAME
                Nickname.TYPE_SHORT_NAME ->
                    nick.type = Contact.NICKNAME_TYPE_SHORT_NAME
                Nickname.TYPE_INITIALS ->
                    nick.type = Contact.NICKNAME_TYPE_INITIALS
                Nickname.TYPE_OTHER_NAME ->
                    nick.type = Contact.NICKNAME_TYPE_OTHER_NAME
                Nickname.TYPE_CUSTOM ->
                    row.getAsString(Nickname.LABEL)?.let { nick.type = labelToXName(it) }
            }

            contact!!.nickName = nick
        }
    }

    protected fun populateNote(row: ContentValues) {
        contact!!.note = row.getAsString(Note.NOTE)
    }

    protected fun populateStructuredPostal(row: ContentValues) {
        val address = Address()
        val labeledAddress = LabeledProperty(address)

        address.label = row.getAsString(StructuredPostal.FORMATTED_ADDRESS)
        when (row.getAsInteger(StructuredPostal.TYPE)) {
            StructuredPostal.TYPE_HOME ->
                address.types += AddressType.HOME
            StructuredPostal.TYPE_WORK ->
                address.types += AddressType.WORK
            StructuredPostal.TYPE_CUSTOM -> {
                row.getAsString(StructuredPostal.LABEL)?.let {
                    labeledAddress.label = it
                    address.types += AddressType.get(labelToXName(it))
                }
            }
        }
        address.streetAddress = row.getAsString(StructuredPostal.STREET)
        address.poBox = row.getAsString(StructuredPostal.POBOX)
        address.extendedAddress = row.getAsString(StructuredPostal.NEIGHBORHOOD)
        address.locality = row.getAsString(StructuredPostal.CITY)
        address.region = row.getAsString(StructuredPostal.REGION)
        address.postalCode = row.getAsString(StructuredPostal.POSTCODE)
        address.country = row.getAsString(StructuredPostal.COUNTRY)
        contact!!.addresses += labeledAddress
    }

    protected fun populateWebsite(row: ContentValues) {
        val url = Url(row.getAsString(Website.URL))
        val labeledUrl = LabeledProperty(url)

        when (row.getAsInteger(Website.TYPE)) {
            Website.TYPE_HOMEPAGE ->
                url.type = Contact.URL_TYPE_HOMEPAGE
            Website.TYPE_BLOG ->
                url.type = Contact.URL_TYPE_BLOG
            Website.TYPE_PROFILE ->
                url.type = Contact.URL_TYPE_PROFILE
            Website.TYPE_HOME ->
                url.type = "home"
            Website.TYPE_WORK ->
                url.type = "work"
            Website.TYPE_FTP ->
                url.type = Contact.URL_TYPE_FTP
            Website.TYPE_CUSTOM ->
                row.getAsString(Website.LABEL)?.let {
                    url.type = labelToXName(it)
                    labeledUrl.label = it
                }
        }
        contact!!.urls += labeledUrl
    }

    protected fun populateEvent(row: ContentValues) {
        val dateStr = row.getAsString(CommonDataKinds.Event.START_DATE)
        var full: Date? = null
        var partial: PartialDate? = null
        val fullFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        try {
            full = fullFormat.parse(dateStr)
        } catch(e: ParseException) {
            try {
                partial = PartialDate.parse(dateStr)
            } catch (e: IllegalArgumentException) {
                Constants.log.log(Level.WARNING, "Couldn't parse birthday/anniversary date from database", e)
            }
        }

        if (full != null || partial != null)
            when (row.getAsInteger(Event.TYPE)) {
                Event.TYPE_ANNIVERSARY ->
                    contact!!.anniversary = if (full != null) Anniversary(full) else Anniversary(partial)
                Event.TYPE_BIRTHDAY ->
                    contact!!.birthDay = if (full != null) Birthday(full) else Birthday(partial)
            }
    }

    protected fun populateRelation(row: ContentValues) {
        row.getAsString(Relation.NAME)?.let { name ->
            val related = Related()
            related.text = name

            when (row.getAsInteger(Relation.TYPE)) {
                Relation.TYPE_ASSISTANT,
                Relation.TYPE_MANAGER ->
                    related.types += RelatedType.CO_WORKER
                Relation.TYPE_BROTHER,
                Relation.TYPE_SISTER ->
                    related.types += RelatedType.SIBLING
                Relation.TYPE_CHILD ->
                    related.types += RelatedType.CHILD
                Relation.TYPE_FRIEND ->
                    related.types += RelatedType.FRIEND
                Relation.TYPE_FATHER,
                Relation.TYPE_MOTHER,
                Relation.TYPE_PARENT ->
                    related.types += RelatedType.PARENT
                Relation.TYPE_DOMESTIC_PARTNER,
                Relation.TYPE_PARTNER,
                Relation.TYPE_SPOUSE ->
                    related.types += RelatedType.SPOUSE
                Relation.TYPE_RELATIVE ->
                    related.types += RelatedType.KIN
                Relation.TYPE_CUSTOM ->
                    row.getAsString(Relation.LABEL)?.split(",")?.forEach {
                        related.types += RelatedType.get(it.trim())
                    }
            }

            contact!!.relations += related
        }
    }

    protected fun populateSipAddress(row: ContentValues) {
        try {
            val impp = Impp("sip:" + row.getAsString(SipAddress.SIP_ADDRESS))
            val labeledImpp = LabeledProperty(impp)

            when (row.getAsInteger(SipAddress.TYPE)) {
                SipAddress.TYPE_HOME ->
                    impp.types += ImppType.HOME
                SipAddress.TYPE_WORK ->
                    impp.types += ImppType.WORK
                SipAddress.TYPE_CUSTOM ->
                    row.getAsString(SipAddress.LABEL)?.let {
                        labeledImpp.label = it
                        impp.types += ImppType.get(labelToXName(it))
                    }
            }
            contact!!.impps.add(labeledImpp)
        } catch(e: IllegalArgumentException) {
            Constants.log.warning("Ignoring invalid locally stored SIP address")
        }
    }

    /**
     * Override this to handle custom data rows, for example to add additional
     * information to [contact].
     * @param mimeType    MIME type of the row
     * @param row         values of the row
     */
    @Throws(ContactsStorageException::class)
    open protected fun populateData(mimeType: String, row: ContentValues) {
    }


    @Throws(ContactsStorageException::class)
    fun create(): Uri {
        val batch = BatchOperation(addressBook.provider)

        val builder = ContentProviderOperation.newInsert(addressBook.syncAdapterURI(RawContacts.CONTENT_URI))
        buildContact(builder, false)
        batch.enqueue(BatchOperation.Operation(builder))

        insertDataRows(batch)

        batch.commit()
        val result = batch.getResult(0) ?: throw ContactsStorageException("Empty result from content provider when adding contact")
        id = ContentUris.parseId(result.uri)

        // we need a raw contact ID to insert the photo
        insertPhoto(contact!!.photo)

        return result.uri
    }

    @Throws(ContactsStorageException::class)
    fun update(contact: Contact): Int {
        this.contact = contact

        val batch = BatchOperation(addressBook.provider)

        val builder = ContentProviderOperation.newUpdate(rawContactSyncURI())
        buildContact(builder, true)
        batch.enqueue(BatchOperation.Operation(builder))

        // delete known data rows before adding the new ones; don't delete group memberships!
        batch.enqueue(BatchOperation.Operation(
                ContentProviderOperation.newDelete(dataSyncURI())
                .withSelection(RawContacts.Data.RAW_CONTACT_ID + "=? AND " + RawContacts.Data.MIMETYPE + " NOT IN (?,?)",
                        arrayOf(id.toString(), GroupMembership.CONTENT_ITEM_TYPE, CachedGroupMembership.CONTENT_ITEM_TYPE))
        ))
        insertDataRows(batch)
        val results = batch.commit()

        insertPhoto(contact.photo)

        return results
    }

    @Throws(ContactsStorageException::class)
    fun delete() =
        try {
            addressBook.provider.delete(rawContactSyncURI(), null, null)
        } catch (e: RemoteException) {
            throw ContactsStorageException("Couldn't delete local contact", e)
        }


    protected fun buildContact(builder: ContentProviderOperation.Builder, update: Boolean) {
        if (!update)
            builder	.withValue(RawContacts.ACCOUNT_NAME, addressBook.account.name)
                    .withValue(RawContacts.ACCOUNT_TYPE, addressBook.account.type)

        builder .withValue(RawContacts.DIRTY, 0)
                .withValue(RawContacts.DELETED, 0)
                .withValue(COLUMN_FILENAME, fileName)
                .withValue(COLUMN_ETAG, eTag)
                .withValue(COLUMN_UID, contact!!.uid)

        Constants.log.log(Level.FINER, "Built RawContact data row", builder.build())
    }


    /**
     * Inserts the data rows for a given raw contact.
     * Override this (and call [super]!) to add custom data rows,
     * for example generated from some properties of [contact].
     * @param  batch    batch operation used to insert the data rows
     * @throws ContactsStorageException on contact provider errors
     */
    @Throws(ContactsStorageException::class)
    open protected fun insertDataRows(batch: BatchOperation) {
        val contact = requireNotNull(contact)

        insertStructuredName(batch)
        insertNickname(batch)
        insertOrganization(batch)

        contact.phoneNumbers.forEach { insertPhoneNumber(batch, it) }
        contact.emails.forEach { insertEmail(batch, it) }
        contact.impps.forEach { insertIMPP(batch, it) }     // handles SIP addresses, too
        contact.addresses.forEach { insertStructuredPostal(batch, it) }

        insertNote(batch)
        contact.urls.forEach { insertWebsite(batch, it) }
        contact.relations.forEach { insertRelation(batch, it) }

        contact.anniversary?.let { insertEvent(batch, Event.TYPE_ANNIVERSARY, it) }
        contact.birthDay?.let { insertEvent(batch, Event.TYPE_BIRTHDAY, it) }
    }

    protected fun insertStructuredName(batch: BatchOperation) {
        val contact = requireNotNull(contact)
        if     (contact.displayName == null &&
                contact.prefix == null &&
                contact.givenName == null && contact.middleName == null && contact.familyName == null &&
                contact.suffix == null &&
                contact.phoneticGivenName == null && contact.phoneticMiddleName == null && contact.phoneticFamilyName == null)
            return

        val op: BatchOperation.Operation
        val builder = ContentProviderOperation.newInsert(dataSyncURI())
        if (id == null)
            op = BatchOperation.Operation(builder, StructuredName.RAW_CONTACT_ID, 0)
        else {
            op = BatchOperation.Operation(builder)
            builder.withValue(StructuredName.RAW_CONTACT_ID, id)
        }
        builder .withValue(RawContacts.Data.MIMETYPE, CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(StructuredName.DISPLAY_NAME, contact.displayName)
                .withValue(StructuredName.PREFIX, contact.prefix)
                .withValue(StructuredName.GIVEN_NAME, contact.givenName)
                .withValue(StructuredName.MIDDLE_NAME, contact.middleName)
                .withValue(StructuredName.FAMILY_NAME, contact.familyName)
                .withValue(StructuredName.SUFFIX, contact.suffix)
                .withValue(StructuredName.PHONETIC_GIVEN_NAME, contact.phoneticGivenName)
                .withValue(StructuredName.PHONETIC_MIDDLE_NAME, contact.phoneticMiddleName)
                .withValue(StructuredName.PHONETIC_FAMILY_NAME, contact.phoneticFamilyName)
        Constants.log.log(Level.FINER, "Built StructuredName data row", builder.build())
        batch.enqueue(op)
    }

    protected fun insertPhoneNumber(batch: BatchOperation, labeledNumber: LabeledProperty<Telephone>) {
        val number = labeledNumber.property

        val types = number.types

        // preferred number?
        var pref: Int? = null
        try {
            pref = number.pref
        } catch(e: IllegalStateException) {
            Constants.log.log(Level.FINER, "Can't understand phone number PREF", e)
        }
        var is_primary = pref != null
        if (types.contains(TelephoneType.PREF)) {
            is_primary = true
            types -= TelephoneType.PREF
        }

        var typeCode: Int = Phone.TYPE_OTHER
        var typeLabel: String? = null
        if (labeledNumber.label != null) {
            typeCode = Phone.TYPE_CUSTOM
            typeLabel = labeledNumber.label
        } else {
            when {
                // 1 Android type <-> 2 VCard types: fax, cell, pager
                types.contains(TelephoneType.FAX) ->
                    typeCode = when {
                        types.contains(TelephoneType.HOME) -> Phone.TYPE_FAX_HOME
                        types.contains(TelephoneType.WORK) -> Phone.TYPE_FAX_WORK
                        else                               -> Phone.TYPE_OTHER_FAX
                    }
                types.contains(TelephoneType.CELL) ->
                    typeCode = if (types.contains(TelephoneType.WORK))
                        Phone.TYPE_WORK_MOBILE
                    else
                        Phone.TYPE_MOBILE
                types.contains(TelephoneType.PAGER) ->
                    typeCode = if (types.contains(TelephoneType.WORK))
                        Phone.TYPE_WORK_PAGER
                    else
                        Phone.TYPE_PAGER

                // types with 1:1 translation
                types.contains(TelephoneType.HOME) ->
                    typeCode = Phone.TYPE_HOME
                types.contains(TelephoneType.WORK) ->
                    typeCode = Phone.TYPE_WORK
                types.contains(Contact.PHONE_TYPE_CALLBACK) ->
                    typeCode = Phone.TYPE_CALLBACK
                types.contains(TelephoneType.CAR) ->
                    typeCode = Phone.TYPE_CAR
                types.contains(Contact.PHONE_TYPE_COMPANY_MAIN) ->
                    typeCode = Phone.TYPE_COMPANY_MAIN
                types.contains(TelephoneType.ISDN) ->
                    typeCode = Phone.TYPE_ISDN
                types.contains(Contact.PHONE_TYPE_RADIO) ->
                    typeCode = Phone.TYPE_RADIO
                types.contains(Contact.PHONE_TYPE_ASSISTANT) ->
                    typeCode = Phone.TYPE_ASSISTANT
                types.contains(Contact.PHONE_TYPE_MMS) ->
                    typeCode = Phone.TYPE_MMS

                types.contains(TelephoneType.VOICE) ||
                types.contains(TelephoneType.TEXT) -> {}

                types.isNotEmpty() -> {
                    val type = types.first()
                    typeCode = Phone.TYPE_CUSTOM
                    typeLabel = xNameToLabel(type.value)
                }
            }
        }

        val op: BatchOperation.Operation
        val builder = ContentProviderOperation.newInsert(dataSyncURI())
        if (id == null)
            op = BatchOperation.Operation(builder, Phone.RAW_CONTACT_ID, 0)
        else {
            op = BatchOperation.Operation(builder)
            builder.withValue(Phone.RAW_CONTACT_ID, id)
        }
        builder	.withValue(Phone.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                .withValue(Phone.NUMBER, number.text)
                .withValue(Phone.TYPE, typeCode)
                .withValue(Phone.LABEL, typeLabel)
                .withValue(Phone.IS_PRIMARY, if (is_primary) 1 else 0)
                .withValue(Phone.IS_SUPER_PRIMARY, if (is_primary) 1 else 0)
        Constants.log.log(Level.FINER, "Built Phone data row", builder.build())
        batch.enqueue(op)
    }

    protected fun insertEmail(batch: BatchOperation, labeledEmail: LabeledProperty<ezvcard.property.Email>) {
        val email = labeledEmail.property

        val types = email.types

        // preferred email address?
        var pref: Int? = null
        try {
            pref = email.pref
        } catch(e: IllegalStateException) {
            Constants.log.log(Level.FINER, "Can't understand email PREF", e)
        }
        var is_primary = pref != null
        if (types.contains(EmailType.PREF)) {
            is_primary = true
            types -= EmailType.PREF
        }

        var typeCode: Int = 0
        var typeLabel: String? = null
        if (labeledEmail.label != null) {
            typeCode = Email.TYPE_CUSTOM
            typeLabel = labeledEmail.label
        } else {
            for (type in types)
                when (type) {
                    EmailType.HOME -> typeCode = Email.TYPE_HOME
                    EmailType.WORK -> typeCode = Email.TYPE_WORK
                    Contact.EMAIL_TYPE_MOBILE -> typeCode = Email.TYPE_MOBILE
                }
            if (typeCode == 0) {
                if (email.types.isEmpty())
                    typeCode = Email.TYPE_OTHER
                else {
                    typeCode = Email.TYPE_CUSTOM
                    typeLabel = xNameToLabel(types.first().value)
                }
            }
        }

        val op: BatchOperation.Operation
        val builder = ContentProviderOperation.newInsert(dataSyncURI())
        if (id == null)
            op = BatchOperation.Operation(builder, Email.RAW_CONTACT_ID, 0)
        else {
            op = BatchOperation.Operation(builder)
            builder.withValue(Email.RAW_CONTACT_ID, id)
        }
        builder	.withValue(Email.MIMETYPE, Email.CONTENT_ITEM_TYPE)
                .withValue(Email.ADDRESS, email.value)
                .withValue(Email.TYPE, typeCode)
                .withValue(Email.LABEL, typeLabel)
                .withValue(Email.IS_PRIMARY, if (is_primary) 1 else 0)
                .withValue(Phone.IS_SUPER_PRIMARY, if (is_primary) 1 else 0)
        Constants.log.log(Level.FINER, "Built Email data row", builder.build())
        batch.enqueue(op)
    }

    protected fun insertOrganization(batch: BatchOperation) {
        val contact = requireNotNull(contact)
        if (contact.organization == null && contact.jobTitle == null && contact.jobDescription == null)
            return

        var company: String? = null
        var department: String? = null
        val organization = contact.organization
        organization?.let {
            val org = it.values.iterator()
            if (org.hasNext())
                company = org.next()
            if (org.hasNext())
                department = org.next()
        }

        val op: BatchOperation.Operation
        val builder = ContentProviderOperation.newInsert(dataSyncURI())
        if (id == null)
            op = BatchOperation.Operation(builder, Organization.RAW_CONTACT_ID, 0)
        else {
            op = BatchOperation.Operation(builder)
            builder.withValue(Organization.RAW_CONTACT_ID, id)
        }
        builder .withValue(Organization.MIMETYPE, Organization.CONTENT_ITEM_TYPE)
                .withValue(Organization.COMPANY, company)
                .withValue(Organization.DEPARTMENT, department)
                .withValue(Organization.TITLE, contact.jobTitle)
                .withValue(Organization.JOB_DESCRIPTION, contact.jobDescription)
        Constants.log.log(Level.FINER, "Built Organization data row", builder.build())
        batch.enqueue(op)
    }

    protected fun insertIMPP(batch: BatchOperation, labeledImpp: LabeledProperty<Impp>) {
        val impp = labeledImpp.property

        var typeCode: Int = Im.TYPE_OTHER
        var typeLabel: String? = null

        if (labeledImpp.label != null) {
            typeCode = Im.TYPE_CUSTOM
            typeLabel = labeledImpp.label
        } else {
            for (type in impp.types)
                when (type) {
                    ImppType.HOME,
                    ImppType.PERSONAL -> typeCode = Im.TYPE_HOME
                    ImppType.WORK,
                    ImppType.BUSINESS -> typeCode = Im.TYPE_WORK
                }
            if (typeCode == Im.TYPE_OTHER && impp.types.isNotEmpty()) {
                typeCode = Im.TYPE_CUSTOM
                typeLabel = xNameToLabel(impp.types.first().value)
            }
        }

        val protocol = impp.protocol
        if (protocol == null) {
            Constants.log.warning("Ignoring IMPP address without protocol")
            return
        }

        var protocolCode = 0
        var protocolLabel: String? = null

        // SIP addresses are IMPP entries in the VCard but locally stored in SipAddress rather than Im
        var sipAddress = false

        when {
            impp.isAim -> protocolCode = Im.PROTOCOL_AIM
            impp.isMsn -> protocolCode = Im.PROTOCOL_MSN
            impp.isYahoo -> protocolCode = Im.PROTOCOL_YAHOO
            impp.isSkype -> protocolCode = Im.PROTOCOL_SKYPE
            protocol.equals("qq", true) -> protocolCode = Im.PROTOCOL_QQ
            protocol.equals("google-talk", true) -> protocolCode = Im.PROTOCOL_GOOGLE_TALK
            impp.isIcq -> protocolCode = Im.PROTOCOL_ICQ
            impp.isXmpp || protocol.equals("jabber", true) -> protocolCode = Im.PROTOCOL_JABBER
            protocol.equals("netmeeting", true) -> protocolCode = Im.PROTOCOL_NETMEETING
            protocol.equals("sip", true) -> sipAddress = true
            else -> {
                protocolCode = Im.PROTOCOL_CUSTOM
                protocolLabel = protocol
            }
        }

        val op: BatchOperation.Operation
        val builder = ContentProviderOperation.newInsert(dataSyncURI())
        if (id == null)
            op = BatchOperation.Operation(builder, Im.RAW_CONTACT_ID, 0)
        else {
            op = BatchOperation.Operation(builder)
            builder.withValue(Im.RAW_CONTACT_ID, id)
        }
        if (sipAddress) {
            // save as SIP address
            builder .withValue(SipAddress.MIMETYPE, SipAddress.CONTENT_ITEM_TYPE)
                    .withValue(SipAddress.DATA, impp.handle)
                    .withValue(SipAddress.TYPE, typeCode)
                    .withValue(SipAddress.LABEL, typeLabel)
            Constants.log.log(Level.FINER, "Built SipAddress data row", builder.build())
        } else {
            // save as IM address
            builder	.withValue(Im.MIMETYPE, Im.CONTENT_ITEM_TYPE)
                    .withValue(Im.DATA, impp.handle)
                    .withValue(Im.TYPE, typeCode)
                    .withValue(Im.LABEL, typeLabel)
                    .withValue(Im.PROTOCOL, protocolCode)
                    .withValue(Im.CUSTOM_PROTOCOL, protocolLabel)
            Constants.log.log(Level.FINER, "Built Im data row", builder.build())
        }
        batch.enqueue(op)
    }

    protected fun insertNickname(batch: BatchOperation) {
        val nick = contact!!.nickName
        if (nick == null || nick.values.isEmpty())
            return

        val typeCode: Int
        var typeLabel: String? = null

        val type = nick.type
        typeCode = when (type) {
            Contact.NICKNAME_TYPE_MAIDEN_NAME -> Nickname.TYPE_MAIDEN_NAME
            Contact.NICKNAME_TYPE_SHORT_NAME ->  Nickname.TYPE_SHORT_NAME
            Contact.NICKNAME_TYPE_INITIALS ->    Nickname.TYPE_INITIALS
            Contact.NICKNAME_TYPE_OTHER_NAME ->  Nickname.TYPE_OTHER_NAME
            null                             ->  Nickname.TYPE_DEFAULT
            else -> {
                typeLabel = xNameToLabel(type)
                Nickname.TYPE_CUSTOM
            }
        }

        val op: BatchOperation.Operation
        val builder = ContentProviderOperation.newInsert(dataSyncURI())
        if (id == null)
            op = BatchOperation.Operation(builder, Nickname.RAW_CONTACT_ID, 0)
        else {
            op = BatchOperation.Operation(builder)
            builder.withValue(Nickname.RAW_CONTACT_ID, id)
        }
        builder .withValue(Nickname.MIMETYPE, Nickname.CONTENT_ITEM_TYPE)
                .withValue(Nickname.NAME, nick.values.first())
                .withValue(Nickname.TYPE, typeCode)
                .withValue(Nickname.LABEL, typeLabel)
        Constants.log.log(Level.FINER, "Built Nickname data row", builder.build())
        batch.enqueue(op)
    }

    protected fun insertNote(batch: BatchOperation) {
        val contact = requireNotNull(contact)
        if (contact.note.isNullOrEmpty())
            return

        val op: BatchOperation.Operation
        val builder = ContentProviderOperation.newInsert(dataSyncURI())
        if (id == null)
            op = BatchOperation.Operation(builder, Note.RAW_CONTACT_ID, 0)
        else {
            op = BatchOperation.Operation(builder)
            builder.withValue(Note.RAW_CONTACT_ID, id)
        }
        builder .withValue(Note.MIMETYPE, Note.CONTENT_ITEM_TYPE)
                .withValue(Note.NOTE, contact.note)
        Constants.log.log(Level.FINER, "Built Note data row", builder.build())
        batch.enqueue(op)
    }

    protected fun insertStructuredPostal(batch: BatchOperation, labeledAddress: LabeledProperty<Address>) {
        val address = labeledAddress.property

        var formattedAddress = address.label
        if (formattedAddress.isNullOrEmpty()) {
            /*	no formatted address from server, built it like this:
             *
             *  street po.box (extended)
             *	postcode city
             *	region
             *	COUNTRY
             */

            val lineStreet = arrayOf(address.streetAddress, address.poBox, address.extendedAddress).filter { it.isNotEmpty() }.joinToString(" ")
            val lineLocality = arrayOf(address.postalCode, address.locality).filter { it.isNotEmpty() }.joinToString(" ")

            val lines = LinkedList<String>()
            if (lineStreet.isNotEmpty())
                lines += lineStreet
            if (lineLocality.isNotEmpty())
                lines += lineLocality
            if (!address.region.isNullOrEmpty())
                lines += address.region
            if (!address.country.isNullOrEmpty())
                lines += address.country.toUpperCase()

            formattedAddress = lines.joinToString("\n")
        }

        var typeCode = StructuredPostal.TYPE_OTHER
        var typeLabel: String? = null
        if (labeledAddress.label != null) {
            typeCode = StructuredPostal.TYPE_CUSTOM
            typeLabel = labeledAddress.label
        } else {
            for (type in address.types)
                when (type) {
                    AddressType.HOME -> typeCode = StructuredPostal.TYPE_HOME
                    AddressType.WORK -> typeCode = StructuredPostal.TYPE_WORK
                }
            if (typeCode == StructuredPostal.TYPE_OTHER && address.types.isNotEmpty()) {
                typeCode = StructuredPostal.TYPE_CUSTOM
                typeLabel = xNameToLabel(address.types.first().value)
            }
        }

        val op: BatchOperation.Operation
        val builder = ContentProviderOperation.newInsert(dataSyncURI())
        if (id == null)
            op = BatchOperation.Operation(builder, StructuredPostal.RAW_CONTACT_ID, 0)
        else {
            op = BatchOperation.Operation(builder)
            builder.withValue(StructuredPostal.RAW_CONTACT_ID, id)
        }
        builder .withValue(StructuredPostal.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE)
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
        Constants.log.log(Level.FINER, "Built StructuredPostal data row", builder.build())
        batch.enqueue(op)
    }

    protected fun insertWebsite(batch: BatchOperation, labeledUrl: LabeledProperty<Url>) {
        val url = labeledUrl.property

        val typeCode: Int
        var typeLabel: String? = null
        if (labeledUrl.label != null) {
            typeCode = Website.TYPE_CUSTOM
            typeLabel = labeledUrl.label
        } else {
            val type = url.type
            typeCode = when (type) {
                Contact.URL_TYPE_HOMEPAGE -> Website.TYPE_HOMEPAGE
                Contact.URL_TYPE_BLOG ->     Website.TYPE_BLOG
                Contact.URL_TYPE_PROFILE ->  Website.TYPE_PROFILE
                "home" ->                    Website.TYPE_HOME
                "work" ->                    Website.TYPE_WORK
                Contact.URL_TYPE_FTP ->      Website.TYPE_FTP
                null ->                      Website.TYPE_OTHER
                else -> {
                    typeLabel = xNameToLabel(type)
                    Website.TYPE_CUSTOM
                }
            }
        }

        val op: BatchOperation.Operation
        val builder = ContentProviderOperation.newInsert(dataSyncURI())
        if (id == null)
            op = BatchOperation.Operation(builder, Website.RAW_CONTACT_ID, 0)
        else {
            op = BatchOperation.Operation(builder)
            builder.withValue(Website.RAW_CONTACT_ID, id)
        }
        builder .withValue(Website.MIMETYPE, Website.CONTENT_ITEM_TYPE)
                .withValue(Website.URL, url.value)
                .withValue(Website.TYPE, typeCode)
                .withValue(Website.LABEL, typeLabel)
        Constants.log.log(Level.FINER, "Built Website data row", builder.build())
        batch.enqueue(op)
    }

    protected fun insertEvent(batch: BatchOperation, type: Int, dateOrTime: DateOrTimeProperty) {
        val dateStr: String
        when {
            dateOrTime.date != null -> {
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                dateStr = format.format(dateOrTime.date)
            }
            dateOrTime.partialDate != null ->
                dateStr = dateOrTime.partialDate.toString()
            else -> {
                Constants.log.log(Level.WARNING, "Ignoring date/time without (partial) date", dateOrTime)
                return
            }
        }

        val op: BatchOperation.Operation
        val builder = ContentProviderOperation.newInsert(dataSyncURI())
        if (id == null)
            op = BatchOperation.Operation(builder, Event.RAW_CONTACT_ID, 0)
        else {
            op = BatchOperation.Operation(builder)
            builder.withValue(Event.RAW_CONTACT_ID, id)
        }
        builder .withValue(Event.MIMETYPE, Event.CONTENT_ITEM_TYPE)
                .withValue(Event.TYPE, type)
                .withValue(Event.START_DATE, dateStr)
        batch.enqueue(op)
        Constants.log.log(Level.FINER, "Built Event data row", builder.build())
    }

    protected fun insertRelation(batch: BatchOperation, related: Related) {
        if (related.text.isNullOrEmpty())
            return

        var typeCode = Relation.TYPE_CUSTOM

        val labels = LinkedList<String>()
        for (type in related.types)
            when (type) {
                RelatedType.CHILD  -> typeCode = Relation.TYPE_CHILD
                RelatedType.SPOUSE -> typeCode = Relation.TYPE_PARTNER
                RelatedType.FRIEND -> typeCode = Relation.TYPE_FRIEND
                RelatedType.KIN    -> typeCode = Relation.TYPE_RELATIVE
                RelatedType.PARENT -> typeCode = Relation.TYPE_PARENT
                else               -> labels += type.value
            }

        val op: BatchOperation.Operation
        val builder = ContentProviderOperation.newInsert(dataSyncURI())
        if (id == null)
            op = BatchOperation.Operation(builder, Relation.RAW_CONTACT_ID, 0)
        else {
            op = BatchOperation.Operation(builder)
            builder.withValue(Relation.RAW_CONTACT_ID, id)
        }
        builder .withValue(Relation.MIMETYPE, Relation.CONTENT_ITEM_TYPE)
                .withValue(Relation.NAME, related.text)
                .withValue(Relation.TYPE, typeCode)
                .withValue(Relation.LABEL, StringUtils.trimToNull(labels.joinToString(", ")))
        Constants.log.log(Level.FINER, "Built Relation data row", builder.build())
        batch.enqueue(op)
    }

    protected fun insertPhoto(orig: ByteArray?) {
        if (orig == null)
            return

        // The following approach would be correct, but it doesn't work:
        // the ContactsProvider handler will process the image in background and update
        // the raw contact with the new photo ID when it's finished, setting it to dirty again!
        // See https://code.google.com/p/android/issues/detail?id=226875

        /*Uri photoUri = addressBook.syncAdapterURI(Uri.withAppendedPath(
                ContentUris.withAppendedId(RawContacts.CONTENT_URI, id),
                RawContacts.DisplayPhoto.CONTENT_DIRECTORY));
        Constants.log.debug("Setting local photo " + photoUri);
        try {
            @Cleanup AssetFileDescriptor fd = addressBook.provider.openAssetFile(photoUri, "w");
            @Cleanup OutputStream stream = fd.createOutputStream();
            if (stream != null)
                stream.write(photo);
            else
                Constants.log.warn("Couldn't create local contact photo file");
        } catch (IOException|RemoteException e) {
            Constants.log.warn("Couldn't write local contact photo file", e);
        }*/

        fun processPhoto(): ByteArray? {
            Constants.log.fine("Processing photo")
            var bitmap = BitmapFactory.decodeByteArray(orig, 0, orig.size)
            if (bitmap == null) {
                Constants.log.warning("Image decoding failed")
                return null
            }

            val width = bitmap.width
            val height = bitmap.height
            val max = photoMaxDimensions.toFloat()

            if (width > max || height > max) {
                val scaleWidth = max/width
                val scaleHeight = max/height
                val scale = Math.min(scaleWidth, scaleHeight)
                val newWidth = (width * scale).toInt()
                val newHeight = (height * scale).toInt()

                Constants.log.fine("Resizing image from ${width}x$height to ${newWidth}x$newHeight")
                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

                val baos = ByteArrayOutputStream()
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 97, baos)) {
                    Constants.log.warning("Couldn't generate contact image in JPEG format")
                    return orig
                }
                return baos.toByteArray()
            }

            return orig
        }

        // We have to write the photo directly into the PHOTO BLOB, which causes
        // a TransactionTooLargeException for photos > 1 MB, so let's scale them down
        processPhoto()?.let { photo ->
            Constants.log.fine("Inserting photo BLOB for raw contact $id")

            val values = ContentValues(3)
            values.put(RawContacts.Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE)
            values.put(Photo.RAW_CONTACT_ID, id)
            values.put(Photo.PHOTO, photo)
            try {
                addressBook.provider.insert(dataSyncURI(), values)
            } catch(e: RemoteException) {
                Constants.log.log(Level.WARNING, "Couldn't insert contact photo", e)
            }
        }
    }


    override fun toString() = ToStringBuilder.reflectionToString(this)!!


    // helpers

    protected fun queryPhotoMaxDimensions(): Int {
        try {
            addressBook.provider.query(ContactsContract.DisplayPhoto.CONTENT_MAX_DIMENSIONS_URI,
                    arrayOf(ContactsContract.DisplayPhoto.DISPLAY_MAX_DIM), null, null, null)?.use { cursor ->
                cursor.moveToFirst()
                return cursor.getInt(0)
            }
        } catch(e: RemoteException) {
            Constants.log.log(Level.SEVERE, "Couldn't get max photo dimensions, assuming 720x720 px", e)
        }
        return 720
    }

    protected fun rawContactSyncURI(): Uri {
        val id = requireNotNull(id)
        return addressBook.syncAdapterURI(ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, id))
    }

    protected fun dataSyncURI() =
            addressBook.syncAdapterURI(ContactsContract.Data.CONTENT_URI)

}

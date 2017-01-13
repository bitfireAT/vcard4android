/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.vcard4android;

import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Entity;
import android.content.EntityIterator;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.Relation;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import ezvcard.parameter.AddressType;
import ezvcard.parameter.EmailType;
import ezvcard.parameter.ImppType;
import ezvcard.parameter.RelatedType;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.Address;
import ezvcard.property.Anniversary;
import ezvcard.property.Birthday;
import ezvcard.property.DateOrTimeProperty;
import ezvcard.property.Impp;
import ezvcard.property.Related;
import ezvcard.property.Telephone;
import ezvcard.property.Url;
import lombok.Cleanup;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@ToString(of={ "id","fileName","eTag" })
public class AndroidContact {
    public static final String
            COLUMN_FILENAME = RawContacts.SOURCE_ID,
            COLUMN_UID = RawContacts.SYNC1,
            COLUMN_ETAG = RawContacts.SYNC2;

    protected final AndroidAddressBook addressBook;

    @Getter
    protected Long id;

    @Getter
    protected String fileName;

    @Getter
    public String eTag;

    protected Contact contact;

    private static int photoMaxDimensions;
    protected synchronized int getPhotoMaxDimensions() {
        if (photoMaxDimensions == 0)
            return photoMaxDimensions = queryPhotoMaxDimensions();
        else
            return photoMaxDimensions;
    }


    protected AndroidContact(@NonNull AndroidAddressBook addressBook, long id, String fileName, String eTag) {
        this.addressBook = addressBook;
        this.id = id;
        this.fileName = fileName;
        this.eTag = eTag;
    }

    protected AndroidContact(@NonNull AndroidAddressBook addressBook, @NonNull Contact contact, String fileName, String eTag) {
        this.addressBook = addressBook;
        this.contact = contact;
        this.fileName = fileName;
        this.eTag = eTag;
    }

    public Contact getContact() throws FileNotFoundException, ContactsStorageException {
        if (contact != null)
            return contact;

        try {
            @Cleanup EntityIterator iter = RawContacts.newEntityIterator(addressBook.provider.query(
                    addressBook.syncAdapterURI(ContactsContract.RawContactsEntity.CONTENT_URI),
                    null, ContactsContract.RawContacts._ID + "=?", new String[] { String.valueOf(id) }, null));

            if (iter.hasNext()) {
                Entity e = iter.next();

                contact = new Contact();
                populateContact(e.getEntityValues());

                List<Entity.NamedContentValues> subValues = e.getSubValues();
                for (Entity.NamedContentValues subValue : subValues) {
                    ContentValues values = subValue.values;
                    String mimeType = values.getAsString(ContactsContract.RawContactsEntity.MIMETYPE);

                    if (mimeType == null) {
                        Constants.log.warning("Ignoring raw contact data row without " + ContactsContract.RawContactsEntity.MIMETYPE);
                        continue;
                    }

                    switch (mimeType) {
                        case StructuredName.CONTENT_ITEM_TYPE:
                            populateStructuredName(values);
                            break;
                        case Phone.CONTENT_ITEM_TYPE:
                            populatePhoneNumber(values);
                            break;
                        case Email.CONTENT_ITEM_TYPE:
                            populateEmail(values);
                            break;
                        case Photo.CONTENT_ITEM_TYPE:
                            populatePhoto(values);
                            break;
                        case Organization.CONTENT_ITEM_TYPE:
                            populateOrganization(values);
                            break;
                        case Im.CONTENT_ITEM_TYPE:
                            populateIMPP(values);
                            break;
                        case Nickname.CONTENT_ITEM_TYPE:
                            populateNickname(values);
                            break;
                        case Note.CONTENT_ITEM_TYPE:
                            populateNote(values);
                            break;
                        case StructuredPostal.CONTENT_ITEM_TYPE:
                            populateStructuredPostal(values);
                            break;
                        case Website.CONTENT_ITEM_TYPE:
                            populateWebsite(values);
                            break;
                        case Event.CONTENT_ITEM_TYPE:
                            populateEvent(values);
                            break;
                        case Relation.CONTENT_ITEM_TYPE:
                            populateRelation(values);
                            break;
                        case SipAddress.CONTENT_ITEM_TYPE:
                            populateSipAddress(values);
                            break;
                        default:
                            populateData(mimeType, values);
                            break;
                    }
                }

                return contact;
            } else
                throw new FileNotFoundException();
        } catch(RemoteException e) {
            throw new ContactsStorageException("Couldn't read local contact", e);
        }
    }

    protected void populateContact(ContentValues row) {
        fileName = row.getAsString(COLUMN_FILENAME);
        eTag = row.getAsString(COLUMN_ETAG);

        contact.uid = row.getAsString(COLUMN_UID);
    }

    protected void populateStructuredName(ContentValues row) {
        contact.displayName = row.getAsString(StructuredName.DISPLAY_NAME);

        contact.prefix = row.getAsString(StructuredName.PREFIX);
        contact.givenName = row.getAsString(StructuredName.GIVEN_NAME);
        contact.middleName = row.getAsString(StructuredName.MIDDLE_NAME);
        contact.familyName = row.getAsString(StructuredName.FAMILY_NAME);
        contact.suffix = row.getAsString(StructuredName.SUFFIX);

        contact.phoneticGivenName = row.getAsString(StructuredName.PHONETIC_GIVEN_NAME);
        contact.phoneticMiddleName = row.getAsString(StructuredName.PHONETIC_MIDDLE_NAME);
        contact.phoneticFamilyName = row.getAsString(StructuredName.PHONETIC_FAMILY_NAME);
    }

    protected void populatePhoneNumber(ContentValues row) {
        Telephone number = new Telephone(row.getAsString(Phone.NUMBER));
        LabeledProperty<Telephone> labeledNumber = new LabeledProperty<>(number);

        Integer type = row.getAsInteger(Phone.TYPE);
        if (type != null)
            switch (type) {
                case Phone.TYPE_HOME:
                    number.getTypes().add(TelephoneType.HOME);
                    break;
                case Phone.TYPE_MOBILE:
                    number.getTypes().add(TelephoneType.CELL);
                    break;
                case Phone.TYPE_WORK:
                    number.getTypes().add(TelephoneType.WORK);
                    break;
                case Phone.TYPE_FAX_WORK:
                    number.getTypes().add(TelephoneType.FAX);
                    number.getTypes().add(TelephoneType.WORK);
                    break;
                case Phone.TYPE_FAX_HOME:
                    number.getTypes().add(TelephoneType.FAX);
                    number.getTypes().add(TelephoneType.HOME);
                    break;
                case Phone.TYPE_PAGER:
                    number.getTypes().add(TelephoneType.PAGER);
                    break;
                case Phone.TYPE_CALLBACK:
                    number.getTypes().add(Contact.PHONE_TYPE_CALLBACK);
                    break;
                case Phone.TYPE_CAR:
                    number.getTypes().add(TelephoneType.CAR);
                    break;
                case Phone.TYPE_COMPANY_MAIN:
                    number.getTypes().add(Contact.PHONE_TYPE_COMPANY_MAIN);
                    break;
                case Phone.TYPE_ISDN:
                    number.getTypes().add(TelephoneType.ISDN);
                    break;
                case Phone.TYPE_MAIN:
                    number.getTypes().add(TelephoneType.VOICE);
                    break;
                case Phone.TYPE_OTHER_FAX:
                    number.getTypes().add(TelephoneType.FAX);
                    break;
                case Phone.TYPE_RADIO:
                    number.getTypes().add(Contact.PHONE_TYPE_RADIO);
                    break;
                case Phone.TYPE_TELEX:
                    number.getTypes().add(TelephoneType.TEXTPHONE);
                    break;
                case Phone.TYPE_TTY_TDD:
                    number.getTypes().add(TelephoneType.TEXT);
                    break;
                case Phone.TYPE_WORK_MOBILE:
                    number.getTypes().add(TelephoneType.CELL);
                    number.getTypes().add(TelephoneType.WORK);
                    break;
                case Phone.TYPE_WORK_PAGER:
                    number.getTypes().add(TelephoneType.PAGER);
                    number.getTypes().add(TelephoneType.WORK);
                    break;
                case Phone.TYPE_ASSISTANT:
                    number.getTypes().add(Contact.PHONE_TYPE_ASSISTANT);
                    break;
                case Phone.TYPE_MMS:
                    number.getTypes().add(Contact.PHONE_TYPE_MMS);
                    break;
                case Phone.TYPE_CUSTOM:
                    String customType = row.getAsString(CommonDataKinds.Phone.LABEL);
                    if (!TextUtils.isEmpty(customType)) {
                        labeledNumber.label = customType;
                        number.getTypes().add(TelephoneType.get(labelToXName(customType)));
                    }
            }
        if (row.getAsInteger(CommonDataKinds.Phone.IS_PRIMARY) != 0)
            number.setPref(1);

        contact.phoneNumbers.add(labeledNumber);
    }

    protected void populateEmail(ContentValues row) {
        ezvcard.property.Email email = new ezvcard.property.Email(row.getAsString(Email.ADDRESS));
        LabeledProperty<ezvcard.property.Email> labeledEmail = new LabeledProperty<>(email);

        Integer type = row.getAsInteger(Email.TYPE);
        if (type != null)
            switch (type) {
                case Email.TYPE_HOME:
                    email.getTypes().add(EmailType.HOME);
                    break;
                case Email.TYPE_WORK:
                    email.getTypes().add(EmailType.WORK);
                    break;
                case Email.TYPE_MOBILE:
                    email.getTypes().add(Contact.EMAIL_TYPE_MOBILE);
                    break;
                case Email.TYPE_CUSTOM:
                    String customType = row.getAsString(Email.LABEL);
                    if (!TextUtils.isEmpty(customType)) {
                        labeledEmail.label = customType;
                        email.getTypes().add(EmailType.get(labelToXName(customType)));
                    }
            }
        if (row.getAsInteger(Email.IS_PRIMARY) != 0)
            email.setPref(1);
        contact.emails.add(labeledEmail);
    }

    protected void populatePhoto(ContentValues row) throws RemoteException {
        if (row.containsKey(Photo.PHOTO_FILE_ID)) {
            Uri photoUri = Uri.withAppendedPath(
                    rawContactSyncURI(),
                    RawContacts.DisplayPhoto.CONTENT_DIRECTORY);
            try {
                @Cleanup AssetFileDescriptor fd = addressBook.provider.openAssetFile(photoUri, "r");
                if (fd != null) {
                    @Cleanup InputStream stream = fd.createInputStream();
                    if (stream != null)
                        contact.photo = IOUtils.toByteArray(stream);
                    else
                        Constants.log.warning("Ignoring inaccessible local contact photo file");
                } else
                    Constants.log.warning("Ignoring non-existent local photo");
            } catch(IOException e) {
                Constants.log.log(Level.WARNING, "Couldn't read local contact photo file", e);
            }
        } else
            contact.photo = row.getAsByteArray(Photo.PHOTO);
    }

    protected void populateOrganization(ContentValues row) {
        String	company = row.getAsString(Organization.COMPANY),
                department = row.getAsString(Organization.DEPARTMENT),
                title = row.getAsString(Organization.TITLE),
                role = row.getAsString(Organization.JOB_DESCRIPTION);

        if (!TextUtils.isEmpty(company) || !TextUtils.isEmpty(department)) {
            ezvcard.property.Organization org = new ezvcard.property.Organization();
            if (!TextUtils.isEmpty(company))
                org.getValues().add(company);
            if (!TextUtils.isEmpty(department))
                org.getValues().add(department);
            contact.organization = org;
        }

        if (!TextUtils.isEmpty(title))
            contact.jobTitle = title;
        if (!TextUtils.isEmpty(role))
            contact.jobDescription = role;
    }

    protected void populateIMPP(ContentValues row) {
        String handle = row.getAsString(Im.DATA);

        if (TextUtils.isEmpty(handle)) {
            Constants.log.warning("Ignoring instant messenger record without handle");
            return;
        }

        Impp impp = null;
        if (row.containsKey(Im.PROTOCOL))
            switch (row.getAsInteger(Im.PROTOCOL)) {
                case Im.PROTOCOL_AIM:
                    impp = Impp.aim(handle);
                    break;
                case Im.PROTOCOL_MSN:
                    impp = Impp.msn(handle);
                    break;
                case Im.PROTOCOL_YAHOO:
                    impp = Impp.yahoo(handle);
                    break;
                case Im.PROTOCOL_SKYPE:
                    impp = Impp.skype(handle);
                    break;
                case Im.PROTOCOL_QQ:
                    impp = new Impp("qq", handle);
                    break;
                case Im.PROTOCOL_GOOGLE_TALK:
                    impp = new Impp("google-talk", handle);
                    break;
                case Im.PROTOCOL_ICQ:
                    impp = Impp.icq(handle);
                    break;
                case Im.PROTOCOL_JABBER:
                    impp = Impp.xmpp(handle);
                    break;
                case Im.PROTOCOL_NETMEETING:
                    impp = new Impp("netmeeting", handle);
                    break;
                case Im.PROTOCOL_CUSTOM:
                    try {
                        impp = new Impp(toURIScheme(row.getAsString(Im.CUSTOM_PROTOCOL)), handle);
                    } catch(IllegalArgumentException e) {
                        Constants.log.warning("Messenger type/value can't be expressed as URI; ignoring");
                    }
            }

        if (impp != null) {
            LabeledProperty<Impp> labeledImpp = new LabeledProperty<>(impp);

            Integer type = row.getAsInteger(Im.TYPE);
            if (type != null)
                switch (type) {
                    case Im.TYPE_HOME:
                        impp.getTypes().add(ImppType.HOME);
                        break;
                    case Im.TYPE_WORK:
                        impp.getTypes().add(ImppType.WORK);
                        break;
                    case Im.TYPE_CUSTOM:
                        String customType = row.getAsString(Im.LABEL);
                        if (!TextUtils.isEmpty(customType)) {
                            labeledImpp.label = customType;
                            impp.getTypes().add(ImppType.get(labelToXName(customType)));
                        }
                }

            contact.impps.add(labeledImpp);
        }
    }

    protected void populateNickname(ContentValues row) {
        if (row.containsKey(Nickname.NAME)) {
            ezvcard.property.Nickname nick = new ezvcard.property.Nickname();

            nick.getValues().add(row.getAsString(Nickname.NAME));

            Integer type = row.getAsInteger(Nickname.TYPE);
            if (type != null)
                switch (type) {
                    case Nickname.TYPE_MAIDEN_NAME:
                        nick.setType(Contact.NICKNAME_TYPE_MAIDEN_NAME);
                        break;
                    case Nickname.TYPE_SHORT_NAME:
                        nick.setType(Contact.NICKNAME_TYPE_SHORT_NAME);
                        break;
                    case Nickname.TYPE_INITIALS:
                        nick.setType(Contact.NICKNAME_TYPE_INITIALS);
                        break;
                    case Nickname.TYPE_OTHER_NAME:
                        nick.setType(Contact.NICKNAME_TYPE_OTHER_NAME);
                        break;
                    case Nickname.TYPE_CUSTOM:
                        String label = row.getAsString(Nickname.LABEL);
                        if (!TextUtils.isEmpty(label))
                            nick.setType(labelToXName(label));
                }

            contact.nickName = nick;
        }
    }

    protected void populateNote(ContentValues row) {
        contact.note = row.getAsString(Note.NOTE);
    }

    protected void populateStructuredPostal(ContentValues row) {
        Address address = new Address();
        LabeledProperty<Address> labeledAddress = new LabeledProperty<>(address);

        address.setLabel(row.getAsString(StructuredPostal.FORMATTED_ADDRESS));
        if (row.containsKey(StructuredPostal.TYPE))
            switch (row.getAsInteger(StructuredPostal.TYPE)) {
                case StructuredPostal.TYPE_HOME:
                    address.getTypes().add(AddressType.HOME);
                    break;
                case StructuredPostal.TYPE_WORK:
                    address.getTypes().add(AddressType.WORK);
                    break;
                case StructuredPostal.TYPE_CUSTOM:
                    String customType = row.getAsString(StructuredPostal.LABEL);
                    if (!TextUtils.isEmpty(customType)) {
                        labeledAddress.label = customType;
                        address.getTypes().add(AddressType.get(labelToXName(customType)));
                    }
                    break;
            }
        address.setStreetAddress(row.getAsString(StructuredPostal.STREET));
        address.setPoBox(row.getAsString(StructuredPostal.POBOX));
        address.setExtendedAddress(row.getAsString(StructuredPostal.NEIGHBORHOOD));
        address.setLocality(row.getAsString(StructuredPostal.CITY));
        address.setRegion(row.getAsString(StructuredPostal.REGION));
        address.setPostalCode(row.getAsString(StructuredPostal.POSTCODE));
        address.setCountry(row.getAsString(StructuredPostal.COUNTRY));
        contact.addresses.add(labeledAddress);
    }

    protected void populateWebsite(ContentValues row) {
        Url url = new Url(row.getAsString(Website.URL));
        LabeledProperty<Url> labeledUrl = new LabeledProperty<>(url);

        if (row.containsKey(Website.TYPE))
            switch (row.getAsInteger(Website.TYPE)) {
                case Website.TYPE_HOMEPAGE:
                    url.setType(Contact.URL_TYPE_HOMEPAGE);
                    break;
                case Website.TYPE_BLOG:
                    url.setType(Contact.URL_TYPE_BLOG);
                    break;
                case Website.TYPE_PROFILE:
                    url.setType(Contact.URL_TYPE_PROFILE);
                    break;
                case Website.TYPE_HOME:
                    url.setType("home");
                    break;
                case Website.TYPE_WORK:
                    url.setType("work");
                    break;
                case Website.TYPE_FTP:
                    url.setType(Contact.URL_TYPE_FTP);
                    break;
                case Website.TYPE_CUSTOM:
                    String label = row.getAsString(Website.LABEL);
                    if (!TextUtils.isEmpty(label)) {
                        url.setType(labelToXName(label));
                        labeledUrl.label = label;
                    }
                    break;
            }
        contact.urls.add(labeledUrl);
    }

    protected void populateEvent(ContentValues row) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        try {
            Date date = formatter.parse(row.getAsString(CommonDataKinds.Event.START_DATE));
            if (row.containsKey(Event.TYPE))
            switch (row.getAsInteger(Event.TYPE)) {
                case Event.TYPE_ANNIVERSARY:
                    contact.anniversary = new Anniversary(date);
                    break;
                case Event.TYPE_BIRTHDAY:
                    contact.birthDay = new Birthday(date);
                    break;
            }
        } catch (ParseException e) {
            Constants.log.log(Level.WARNING, "Couldn't parse birthday/anniversary date from database", e);
        }
    }

    protected void populateRelation(ContentValues row) {
        String name = row.getAsString(Relation.NAME);

        if (TextUtils.isEmpty(name))
            return;

        Related related = new Related();
        related.setText(name);

        if (row.containsKey(Relation.TYPE))
            switch (row.getAsInteger(Relation.TYPE)) {
                case Relation.TYPE_ASSISTANT:
                case Relation.TYPE_MANAGER:
                    related.getTypes().add(RelatedType.CO_WORKER);
                    break;
                case Relation.TYPE_BROTHER:
                case Relation.TYPE_SISTER:
                    related.getTypes().add(RelatedType.SIBLING);
                    break;
                case Relation.TYPE_CHILD:
                    related.getTypes().add(RelatedType.CHILD);
                    break;
                case Relation.TYPE_DOMESTIC_PARTNER:
                    related.getTypes().add(RelatedType.CO_RESIDENT);
                    break;
                case Relation.TYPE_FRIEND:
                    related.getTypes().add(RelatedType.FRIEND);
                    break;
                case Relation.TYPE_FATHER:
                case Relation.TYPE_MOTHER:
                case Relation.TYPE_PARENT:
                    related.getTypes().add(RelatedType.PARENT);
                    break;
                case Relation.TYPE_PARTNER:
                case Relation.TYPE_SPOUSE:
                    related.getTypes().add(RelatedType.SWEETHEART);
                    break;
                case Relation.TYPE_RELATIVE:
                    related.getTypes().add(RelatedType.KIN);
                    break;
            }

        contact.relations.add(related);
    }

    protected void populateSipAddress(ContentValues row) {
        try {
            Impp impp = new Impp("sip:" + row.getAsString(SipAddress.SIP_ADDRESS));
            LabeledProperty<Impp> labeledImpp = new LabeledProperty<>(impp);

            if (row.containsKey(SipAddress.TYPE))
                switch (row.getAsInteger(SipAddress.TYPE)) {
                    case SipAddress.TYPE_HOME:
                        impp.getTypes().add(ImppType.HOME);
                        break;
                    case SipAddress.TYPE_WORK:
                        impp.getTypes().add(ImppType.WORK);
                        break;
                    case SipAddress.TYPE_CUSTOM:
                        String customType = row.getAsString(SipAddress.LABEL);
                        if (!TextUtils.isEmpty(customType)) {
                            labeledImpp.label = customType;
                            impp.getTypes().add(ImppType.get(labelToXName(customType)));
                        }
                }
            contact.impps.add(labeledImpp);
        } catch(IllegalArgumentException e) {
            Constants.log.warning("Ignoring invalid locally stored SIP address");
        }
    }

    /**
     * Override this to handle custom data rows, for example to add additional
     * information to {@link #contact}.
     * @param mimeType    MIME type of the row
     * @param row         values of the row
     */
    protected void populateData(String mimeType, ContentValues row) {
    }


    public Uri create() throws ContactsStorageException {
        BatchOperation batch = new BatchOperation(addressBook.provider);

        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(addressBook.syncAdapterURI(RawContacts.CONTENT_URI));
        buildContact(builder, false);
        batch.enqueue(new BatchOperation.Operation(builder));

        insertDataRows(batch);

        batch.commit();
        Uri uri = batch.getResult(0).uri;
        id = ContentUris.parseId(uri);

        // we need a raw contact ID to insert the photo
        insertPhoto(contact.photo);

        return uri;
    }

    public int update(Contact contact) throws ContactsStorageException {
        this.contact = contact;

        BatchOperation batch = new BatchOperation(addressBook.provider);

        ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(rawContactSyncURI());
        buildContact(builder, true);
        batch.enqueue(new BatchOperation.Operation(builder));

        // delete known data rows before adding the new ones; don't delete group memberships!
        Uri dataRowsUri = addressBook.syncAdapterURI(ContactsContract.Data.CONTENT_URI);
        batch.enqueue(new BatchOperation.Operation(
                ContentProviderOperation.newDelete(dataRowsUri)
                .withSelection(RawContacts.Data.RAW_CONTACT_ID + "=? AND " + RawContacts.Data.MIMETYPE + " NOT IN (?,?)",
                        new String[] { String.valueOf(id), GroupMembership.CONTENT_ITEM_TYPE, CachedGroupMembership.CONTENT_ITEM_TYPE })
        ));
        insertDataRows(batch);
        int results = batch.commit();

        insertPhoto(contact.photo);

        return results;
    }

    public int delete() throws ContactsStorageException {
        try {
            return addressBook.provider.delete(rawContactSyncURI(), null, null);
        } catch (RemoteException e) {
            throw new ContactsStorageException("Couldn't delete local contact", e);
        }
    }

    protected void buildContact(ContentProviderOperation.Builder builder, boolean update) {
        if (!update)
            builder	.withValue(RawContacts.ACCOUNT_NAME, addressBook.account.name)
                    .withValue(RawContacts.ACCOUNT_TYPE, addressBook.account.type);

        builder .withValue(RawContacts.DIRTY, 0)
                .withValue(RawContacts.DELETED, 0)
                .withValue(COLUMN_FILENAME, fileName)
                .withValue(COLUMN_ETAG, eTag)
                .withValue(COLUMN_UID, contact.uid);

        Constants.log.log(Level.FINER, "Built RawContact data row", builder.build());
    }


    /**
     * Inserts the data rows for a given raw contact.
     * Override this (and call {code super}!) to add custom data rows,
     * for example generated from some properties of {@link #contact}.
     * @param batch    batch operation used to insert the data rows
     * @throws ContactsStorageException on contact provider errors
     */
    protected void insertDataRows(BatchOperation batch) throws ContactsStorageException {
        insertStructuredName(batch);

        for (LabeledProperty<Telephone> number : contact.phoneNumbers)
            insertPhoneNumber(batch, number);

        for (LabeledProperty<ezvcard.property.Email> email : contact.emails)
            insertEmail(batch, email);

        insertOrganization(batch);

        for (LabeledProperty<Impp> impp : contact.impps)        // handles SIP addresses, too
            insertIMPP(batch, impp);

        insertNickname(batch);

        insertNote(batch);

        for (LabeledProperty<Address> address : contact.addresses)
            insertStructuredPostal(batch, address);

        for (LabeledProperty<Url> url : contact.urls)
            insertWebsite(batch, url);

        if (contact.anniversary != null)
            insertEvent(batch, Event.TYPE_ANNIVERSARY, contact.anniversary);
        if (contact.birthDay != null)
            insertEvent(batch, Event.TYPE_BIRTHDAY, contact.birthDay);

        for (Related related : contact.relations)
            insertRelation(batch, related);
    }

    protected void insertStructuredName(BatchOperation batch) {
        if (contact.displayName == null &&
                contact.prefix == null &&
                contact.givenName == null && contact.middleName == null && contact.familyName == null &&
                contact.suffix == null &&
                contact.phoneticGivenName == null && contact.phoneticMiddleName == null && contact.phoneticFamilyName == null)
            return;

        final BatchOperation.Operation op;
        final ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(dataSyncURI());
        if (id == null)
            op = new BatchOperation.Operation(builder, StructuredName.RAW_CONTACT_ID, 0);
        else {
            op = new BatchOperation.Operation(builder);
            builder.withValue(StructuredName.RAW_CONTACT_ID, id);
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
                .withValue(StructuredName.PHONETIC_FAMILY_NAME, contact.phoneticFamilyName);
        Constants.log.log(Level.FINER, "Built StructuredName data row", builder.build());
        batch.enqueue(op);
    }

    protected void insertPhoneNumber(BatchOperation batch, LabeledProperty<Telephone> labeledNumber) {
        Telephone number = labeledNumber.property;

        int typeCode = Phone.TYPE_OTHER;
        String typeLabel = null;

        List<TelephoneType> types = number.getTypes();

        // preferred number?
        Integer pref = null;
        try {
            pref = number.getPref();
        } catch(IllegalStateException e) {
            Constants.log.log(Level.FINER, "Can't understand phone number PREF", e);
        }
        boolean is_primary = pref != null;
        if (types.contains(TelephoneType.PREF)) {
            is_primary = true;
            types.remove(TelephoneType.PREF);
        }

        if (labeledNumber.label != null) {
            typeCode = Phone.TYPE_CUSTOM;
            typeLabel = labeledNumber.label;
        } else {
            // 1 Android type <-> 2 VCard types: fax, cell, pager
            if (types.contains(TelephoneType.FAX)) {
                if (types.contains(TelephoneType.HOME))
                    typeCode = Phone.TYPE_FAX_HOME;
                else if (types.contains(TelephoneType.WORK))
                    typeCode = Phone.TYPE_FAX_WORK;
                else
                    typeCode = Phone.TYPE_OTHER_FAX;
            } else if (types.contains(TelephoneType.CELL)) {
                if (types.contains(TelephoneType.WORK))
                    typeCode = Phone.TYPE_WORK_MOBILE;
                else
                    typeCode = Phone.TYPE_MOBILE;
            } else if (types.contains(TelephoneType.PAGER)) {
                if (types.contains(TelephoneType.WORK))
                    typeCode = Phone.TYPE_WORK_PAGER;
                else
                    typeCode = Phone.TYPE_PAGER;
                // types with 1:1 translation
            } else if (types.contains(TelephoneType.HOME)) {
                typeCode = Phone.TYPE_HOME;
            } else if (types.contains(TelephoneType.WORK)) {
                typeCode = Phone.TYPE_WORK;
            } else if (types.contains(Contact.PHONE_TYPE_CALLBACK)) {
                typeCode = Phone.TYPE_CALLBACK;
            } else if (types.contains(TelephoneType.CAR)) {
                typeCode = Phone.TYPE_CAR;
            } else if (types.contains(Contact.PHONE_TYPE_COMPANY_MAIN)) {
                typeCode = Phone.TYPE_COMPANY_MAIN;
            } else if (types.contains(TelephoneType.ISDN)) {
                typeCode = Phone.TYPE_ISDN;
            } else if (types.contains(TelephoneType.VOICE)) {
                typeCode = Phone.TYPE_MAIN;
            } else if (types.contains(Contact.PHONE_TYPE_RADIO)) {
                typeCode = Phone.TYPE_RADIO;
            } else if (types.contains(TelephoneType.TEXTPHONE)) {
                typeCode = Phone.TYPE_TELEX;
            } else if (types.contains(TelephoneType.TEXT)) {
                typeCode = Phone.TYPE_TTY_TDD;
            } else if (types.contains(Contact.PHONE_TYPE_ASSISTANT)) {
                typeCode = Phone.TYPE_ASSISTANT;
            } else if (types.contains(Contact.PHONE_TYPE_MMS)) {
                typeCode = Phone.TYPE_MMS;
            } else if (!types.isEmpty()) {
                TelephoneType type = types.iterator().next();
                typeCode = Phone.TYPE_CUSTOM;
                typeLabel = xNameToLabel(type.getValue());
            }
        }

        final BatchOperation.Operation op;
        final ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(dataSyncURI());
        if (id == null)
            op = new BatchOperation.Operation(builder, Phone.RAW_CONTACT_ID, 0);
        else {
            op = new BatchOperation.Operation(builder);
            builder.withValue(Phone.RAW_CONTACT_ID, id);
        }
        builder	.withValue(Phone.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                .withValue(Phone.NUMBER, number.getText())
                .withValue(Phone.TYPE, typeCode)
                .withValue(Phone.LABEL, typeLabel)
                .withValue(Phone.IS_PRIMARY, is_primary ? 1 : 0)
                .withValue(Phone.IS_SUPER_PRIMARY, is_primary ? 1 : 0);
        Constants.log.log(Level.FINER, "Built Phone data row", builder.build());
        batch.enqueue(op);
    }

    protected void insertEmail(BatchOperation batch, LabeledProperty<ezvcard.property.Email> labeledEmail) {
        ezvcard.property.Email email = labeledEmail.property;

        int typeCode = 0;
        String typeLabel = null;

        List<EmailType> types = email.getTypes();

        // preferred email address?
        Integer pref = null;
        try {
            pref = email.getPref();
        } catch(IllegalStateException e) {
            Constants.log.log(Level.FINER, "Can't understand email PREF", e);
        }
        boolean is_primary = pref != null;
        if (types.contains(EmailType.PREF)) {
            is_primary = true;
            types.remove(EmailType.PREF);
        }

        if (labeledEmail.label != null) {
            typeCode = Email.TYPE_CUSTOM;
            typeLabel = labeledEmail.label;
        } else {
            for (EmailType type : types)
                if (type == EmailType.HOME)
                    typeCode = Email.TYPE_HOME;
                else if (type == EmailType.WORK)
                    typeCode = Email.TYPE_WORK;
                else if (type == Contact.EMAIL_TYPE_MOBILE)
                    typeCode = Email.TYPE_MOBILE;
            if (typeCode == 0) {
                if (email.getTypes().isEmpty())
                    typeCode = Email.TYPE_OTHER;
                else {
                    typeCode = Email.TYPE_CUSTOM;
                    typeLabel = xNameToLabel(email.getTypes().iterator().next().getValue());
                }
            }
        }

        final BatchOperation.Operation op;
        final ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(dataSyncURI());
        if (id == null)
            op = new BatchOperation.Operation(builder, Email.RAW_CONTACT_ID, 0);
        else {
            op = new BatchOperation.Operation(builder);
            builder.withValue(Email.RAW_CONTACT_ID, id);
        }
        builder	.withValue(Email.MIMETYPE, Email.CONTENT_ITEM_TYPE)
                .withValue(Email.ADDRESS, email.getValue())
                .withValue(Email.TYPE, typeCode)
                .withValue(Email.LABEL, typeLabel)
                .withValue(Email.IS_PRIMARY, is_primary ? 1 : 0)
                .withValue(Phone.IS_SUPER_PRIMARY, is_primary ? 1 : 0);
        Constants.log.log(Level.FINER, "Built Email data row", builder.build());
        batch.enqueue(op);
    }

    protected void insertOrganization(BatchOperation batch) {
        if (contact.organization == null && contact.jobTitle == null && contact.jobDescription == null)
            return;

        String company = null, department = null;
        ezvcard.property.Organization organization = contact.organization;
        if (organization != null) {
            Iterator<String> org = organization.getValues().iterator();
            if (org.hasNext())
                company = org.next();
            if (org.hasNext())
                department = org.next();
        }

        final BatchOperation.Operation op;
        final ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(dataSyncURI());
        if (id == null)
            op = new BatchOperation.Operation(builder, Organization.RAW_CONTACT_ID, 0);
        else {
            op = new BatchOperation.Operation(builder);
            builder.withValue(Organization.RAW_CONTACT_ID, id);
        }
        builder .withValue(Organization.MIMETYPE, Organization.CONTENT_ITEM_TYPE)
                .withValue(Organization.COMPANY, company)
                .withValue(Organization.DEPARTMENT, department)
                .withValue(Organization.TITLE, contact.jobTitle)
                .withValue(Organization.JOB_DESCRIPTION, contact.jobDescription);
        Constants.log.log(Level.FINER, "Built Organization data row", builder.build());
        batch.enqueue(op);
    }

    protected void insertIMPP(BatchOperation batch, LabeledProperty<Impp> labeledImpp) {
        Impp impp = labeledImpp.property;

        int typeCode = Im.TYPE_OTHER;       // default value
        String typeLabel = null;

        if (labeledImpp.label != null) {
            typeCode = Im.TYPE_CUSTOM;
            typeLabel = labeledImpp.label;
        } else {
            for (ImppType type : impp.getTypes())
                if (type == ImppType.HOME) {
                    typeCode = Im.TYPE_HOME;
                    break;
                } else if (type == ImppType.WORK || type == ImppType.BUSINESS) {
                    typeCode = Im.TYPE_WORK;
                    break;
                }

            if (typeCode == Im.TYPE_OTHER)      // still default value?
                if (!impp.getTypes().isEmpty()) {
                    typeCode = Im.TYPE_CUSTOM;
                    typeLabel = xNameToLabel(impp.getTypes().iterator().next().getValue());
                }
        }

        String protocol = impp.getProtocol();
        if (protocol == null) {
            Constants.log.warning("Ignoring IMPP address without protocol");
            return;
        }

        int protocolCode = 0;
        String protocolLabel = null;

        // SIP addresses are IMPP entries in the VCard but locally stored in SipAddress rather than Im
        boolean sipAddress = false;

        if (impp.isAim())
            protocolCode = Im.PROTOCOL_AIM;
        else if (impp.isMsn())
            protocolCode = Im.PROTOCOL_MSN;
        else if (impp.isYahoo())
            protocolCode = Im.PROTOCOL_YAHOO;
        else if (impp.isSkype())
            protocolCode = Im.PROTOCOL_SKYPE;
        else if (protocol.equalsIgnoreCase("qq"))
            protocolCode = Im.PROTOCOL_QQ;
        else if (protocol.equalsIgnoreCase("google-talk"))
            protocolCode = Im.PROTOCOL_GOOGLE_TALK;
        else if (impp.isIcq())
            protocolCode = Im.PROTOCOL_ICQ;
        else if (impp.isXmpp() || protocol.equalsIgnoreCase("jabber"))
            protocolCode = Im.PROTOCOL_JABBER;
        else if (protocol.equalsIgnoreCase("netmeeting"))
            protocolCode = Im.PROTOCOL_NETMEETING;
        else if (protocol.equalsIgnoreCase("sip"))
            sipAddress = true;
        else {
            protocolCode = Im.PROTOCOL_CUSTOM;
            protocolLabel = protocol;
        }

        final BatchOperation.Operation op;
        final ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(dataSyncURI());
        if (id == null)
            op = new BatchOperation.Operation(builder, Im.RAW_CONTACT_ID, 0);
        else {
            op = new BatchOperation.Operation(builder);
            builder.withValue(Im.RAW_CONTACT_ID, id);
        }
        if (sipAddress) {
            // save as SIP address
            builder .withValue(SipAddress.MIMETYPE, SipAddress.CONTENT_ITEM_TYPE)
                    .withValue(SipAddress.DATA, impp.getHandle())
                    .withValue(SipAddress.TYPE, typeCode)
                    .withValue(SipAddress.LABEL, typeLabel);
            Constants.log.log(Level.FINER, "Built SipAddress data row", builder.build());
        } else {
            // save as IM address
            builder	.withValue(Im.MIMETYPE, Im.CONTENT_ITEM_TYPE)
                    .withValue(Im.DATA, impp.getHandle())
                    .withValue(Im.TYPE, typeCode)
                    .withValue(Im.LABEL, typeLabel)
                    .withValue(Im.PROTOCOL, protocolCode)
                    .withValue(Im.CUSTOM_PROTOCOL, protocolLabel);
            Constants.log.log(Level.FINER, "Built Im data row", builder.build());
        }
        batch.enqueue(op);
    }

    protected void insertNickname(BatchOperation batch) {
        ezvcard.property.Nickname nick = contact.nickName;
        if (nick != null && !nick.getValues().isEmpty()) {
            int typeCode = Nickname.TYPE_DEFAULT;
            String typeLabel = null;

            String type = nick.getType();
            if (!TextUtils.isEmpty(type))
                switch (type) {
                    case Contact.NICKNAME_TYPE_MAIDEN_NAME:
                        typeCode = Nickname.TYPE_MAIDEN_NAME;
                        break;
                    case Contact.NICKNAME_TYPE_SHORT_NAME:
                        typeCode = Nickname.TYPE_SHORT_NAME;
                        break;
                    case Contact.NICKNAME_TYPE_INITIALS:
                        typeCode = Nickname.TYPE_INITIALS;
                        break;
                    case Contact.NICKNAME_TYPE_OTHER_NAME:
                        typeCode = Nickname.TYPE_OTHER_NAME;
                        break;
                    default:
                        typeCode = Nickname.TYPE_CUSTOM;
                        typeLabel = xNameToLabel(type);
                }

            final BatchOperation.Operation op;
            final ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(dataSyncURI());
            if (id == null)
                op = new BatchOperation.Operation(builder, Nickname.RAW_CONTACT_ID, 0);
            else {
                op = new BatchOperation.Operation(builder);
                builder.withValue(Nickname.RAW_CONTACT_ID, id);
            }
            builder .withValue(Nickname.MIMETYPE, Nickname.CONTENT_ITEM_TYPE)
                    .withValue(Nickname.NAME, nick.getValues().get(0))
                    .withValue(Nickname.TYPE, typeCode)
                    .withValue(Nickname.LABEL, typeLabel);
            Constants.log.log(Level.FINER, "Built Nickname data row", builder.build());
            batch.enqueue(op);
        }
    }

    protected void insertNote(BatchOperation batch) {
        if (!TextUtils.isEmpty(contact.note)) {
            final BatchOperation.Operation op;
            final ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(dataSyncURI());
            if (id == null)
                op = new BatchOperation.Operation(builder, Note.RAW_CONTACT_ID, 0);
            else {
                op = new BatchOperation.Operation(builder);
                builder.withValue(Note.RAW_CONTACT_ID, id);
            }
            builder .withValue(Note.MIMETYPE, Note.CONTENT_ITEM_TYPE)
                    .withValue(Note.NOTE, contact.note);
            Constants.log.log(Level.FINER, "Built Note data row", builder.build());
            batch.enqueue(op);
        }
    }

    protected void insertStructuredPostal(BatchOperation batch, LabeledProperty<Address> labeledAddress) {
        Address address = labeledAddress.property;

        /*	street po.box (extended)
         *	postcode city
         *	region
         *	COUNTRY
         */
        String formattedAddress = address.getLabel();
        if (TextUtils.isEmpty(formattedAddress)) {
            String  lineStreet = StringUtils.join(dropEmpty(new String[] { address.getStreetAddress(), address.getPoBox(), address.getExtendedAddress() }), " "),
                    lineLocality = StringUtils.join(dropEmpty(new String[] { address.getPostalCode(), address.getLocality() }), " ");

            List<String> lines = new LinkedList<>();
            if (!TextUtils.isEmpty(lineStreet))
                lines.add(lineStreet);
            if (!TextUtils.isEmpty(lineLocality))
                lines.add(lineLocality);
            if (!TextUtils.isEmpty(address.getRegion()))
                lines.add(address.getRegion());
            if (!TextUtils.isEmpty(address.getCountry()))
                lines.add(address.getCountry().toUpperCase(Locale.getDefault()));

            formattedAddress = StringUtils.join(lines, "\n");
        }

        int typeCode = StructuredPostal.TYPE_OTHER;
        String typeLabel = null;
        if (labeledAddress.label != null) {
            typeCode = StructuredPostal.TYPE_CUSTOM;
            typeLabel = labeledAddress.label;
        } else {
            for (AddressType type : address.getTypes())
                if (type == AddressType.HOME) {
                    typeCode = StructuredPostal.TYPE_HOME;
                    break;
                } else if (type == AddressType.WORK) {
                    typeCode = StructuredPostal.TYPE_WORK;
                    break;
                }

            if (typeCode == StructuredPostal.TYPE_OTHER)        // still default value?
                if (!address.getTypes().isEmpty()) {
                    typeCode = StructuredPostal.TYPE_CUSTOM;
                    typeLabel = xNameToLabel(address.getTypes().iterator().next().getValue());
                }
        }

        final BatchOperation.Operation op;
        final ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(dataSyncURI());
        if (id == null)
            op = new BatchOperation.Operation(builder, StructuredPostal.RAW_CONTACT_ID, 0);
        else {
            op = new BatchOperation.Operation(builder);
            builder.withValue(StructuredPostal.RAW_CONTACT_ID, id);
        }
        builder .withValue(StructuredPostal.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE)
                .withValue(StructuredPostal.FORMATTED_ADDRESS, formattedAddress)
                .withValue(StructuredPostal.TYPE, typeCode)
                .withValue(StructuredPostal.LABEL, typeLabel)
                .withValue(StructuredPostal.STREET, address.getStreetAddress())
                .withValue(StructuredPostal.POBOX, address.getPoBox())
                .withValue(StructuredPostal.NEIGHBORHOOD, address.getExtendedAddress())
                .withValue(StructuredPostal.CITY, address.getLocality())
                .withValue(StructuredPostal.REGION, address.getRegion())
                .withValue(StructuredPostal.POSTCODE, address.getPostalCode())
                .withValue(StructuredPostal.COUNTRY, address.getCountry());
        Constants.log.log(Level.FINER, "Built StructuredPostal data row", builder.build());
        batch.enqueue(op);
    }

    protected void insertWebsite(BatchOperation batch, LabeledProperty<Url> labeledUrl) {
        Url url = labeledUrl.property;

        int typeCode = Website.TYPE_OTHER;
        String typeLabel = null;
        if (labeledUrl.label != null) {
            typeCode = Website.TYPE_CUSTOM;
            typeLabel = labeledUrl.label;
        } else {
            String type = url.getType();
            if (!TextUtils.isEmpty(type))
                switch (type) {
                    case Contact.URL_TYPE_HOMEPAGE:
                        typeCode = Website.TYPE_HOMEPAGE;
                        break;
                    case Contact.URL_TYPE_BLOG:
                        typeCode = Website.TYPE_BLOG;
                        break;
                    case Contact.URL_TYPE_PROFILE:
                        typeCode = Website.TYPE_PROFILE;
                        break;
                    case "home":
                        typeCode = Website.TYPE_HOME;
                        break;
                    case "work":
                        typeCode = Website.TYPE_WORK;
                        break;
                    case Contact.URL_TYPE_FTP:
                        typeCode = Website.TYPE_FTP;
                        break;
                    default:
                        typeCode = Website.TYPE_CUSTOM;
                        typeLabel = xNameToLabel(type);
                }
        }

        final BatchOperation.Operation op;
        final ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(dataSyncURI());
        if (id == null)
            op = new BatchOperation.Operation(builder, Website.RAW_CONTACT_ID, 0);
        else {
            op = new BatchOperation.Operation(builder);
            builder.withValue(Website.RAW_CONTACT_ID, id);
        }
        builder .withValue(Website.MIMETYPE, Website.CONTENT_ITEM_TYPE)
                .withValue(Website.URL, url.getValue())
                .withValue(Website.TYPE, typeCode)
                .withValue(Website.LABEL, typeLabel);
        Constants.log.log(Level.FINER, "Built Website data row", builder.build());
        batch.enqueue(op);
    }

    protected void insertEvent(BatchOperation batch, int type, DateOrTimeProperty dateOrTime) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        if (dateOrTime.getDate() == null) {
            Constants.log.warning("Ignoring contact event (birthday/anniversary) without date");
            return;
        }

        final BatchOperation.Operation op;
        final ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(dataSyncURI());
        if (id == null)
            op = new BatchOperation.Operation(builder, Event.RAW_CONTACT_ID, 0);
        else {
            op = new BatchOperation.Operation(builder);
            builder.withValue(Event.RAW_CONTACT_ID, id);
        }
        builder .withValue(Event.MIMETYPE, Event.CONTENT_ITEM_TYPE)
                .withValue(Event.TYPE, type)
                .withValue(Event.START_DATE, formatter.format(dateOrTime.getDate()));
        batch.enqueue(op);
        Constants.log.log(Level.FINER, "Built Event data row", builder.build());
    }

    protected void insertRelation(BatchOperation batch, Related related) {
        if (TextUtils.isEmpty(related.getText()))
            return;

        int typeCode = Event.TYPE_CUSTOM;

        List<String> labels = new LinkedList<>();
        for (RelatedType type : related.getTypes()) {
            if (type == RelatedType.CHILD)
                typeCode = Relation.TYPE_CHILD;
            else if (type == RelatedType.CO_RESIDENT)
                typeCode = Relation.TYPE_DOMESTIC_PARTNER;
            else if (type == RelatedType.CRUSH || type == RelatedType.DATE || type == RelatedType.SPOUSE || type == RelatedType.SWEETHEART)
                typeCode = Relation.TYPE_PARTNER;
            else if (type == RelatedType.FRIEND)
                typeCode = Relation.TYPE_FRIEND;
            else if (type == RelatedType.KIN)
                typeCode = Relation.TYPE_RELATIVE;
            else if (type == RelatedType.PARENT)
                typeCode = Relation.TYPE_PARENT;
            else
                labels.add(type.getValue());
        }

        final BatchOperation.Operation op;
        final ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(dataSyncURI());
        if (id == null)
            op = new BatchOperation.Operation(builder, Relation.RAW_CONTACT_ID, 0);
        else {
            op = new BatchOperation.Operation(builder);
            builder.withValue(Relation.RAW_CONTACT_ID, id);
        }
        builder .withValue(Relation.MIMETYPE, Relation.CONTENT_ITEM_TYPE)
                .withValue(Relation.NAME, related.getText())
                .withValue(Relation.TYPE, typeCode)
                .withValue(Relation.LABEL, StringUtils.join(labels, "/"));
        Constants.log.log(Level.FINER, "Built Relation data row", builder.build());
        batch.enqueue(op);
    }

    protected void insertPhoto(byte[] photo) {
        if (photo != null) {
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

            // So we have to write the photo directly into the PHOTO BLOB, which causes
            // a TransactionTooLargeException for photos > 1 MB, so let's scale them down
            photo = processPhoto(photo);

            if (photo != null) {
                Constants.log.fine("Inserting photo for raw contact " + id);

                ContentValues values = new ContentValues(2);
                values.put(RawContacts.Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
                values.put(Photo.RAW_CONTACT_ID, id);
                values.put(Photo.PHOTO, photo);
                try {
                    addressBook.provider.insert(dataSyncURI(), values);
                } catch(RemoteException e) {
                    Constants.log.log(Level.WARNING, "Couldn't set local contact photo, ignoring", e);
                }
            }
        }
    }


    protected static String[] dropEmpty(String[] strings) {
        ArrayList<String> list = new ArrayList<>(strings.length);
        for (String s : strings)
            if (!StringUtils.isEmpty(s))
                list.add(s);
        return list.toArray(new String[list.size()]);
    }

    protected static String labelToXName(String label) {
        return "X-" + label.replaceAll(" ","_").replaceAll("[^\\p{L}\\p{Nd}\\-_]", "").toUpperCase(Locale.US);
    }

    protected static String xNameToLabel(String xname) {
        // "X-MY_PROPERTY"
        String s = xname.toLowerCase(Locale.US);    // 1. ensure lower case -> "x-my_property"
        if (s.startsWith("x-"))                     // 2. remove x- from beginning -> "my_property"
            s = s.substring(2);
        s = s.replace('_', ' ');                    // 3. replace "_" by " " -> "my property"
        return WordUtils.capitalize(s);             // 4. capitalize -> "My Property"
    }

    protected static String toURIScheme(String s) {
        // RFC 3986 3.1
        // scheme      = ALPHA *( ALPHA / DIGIT / "+" / "-" / "." )
        // ALPHA       =  %x41-5A / %x61-7A   ; A-Z / a-z
        // DIGIT       =  %x30-39             ; 0-9
        return s.replaceAll("^[^a-zA-Z]+", "").replaceAll("[^\\da-zA-Z+-.]", "");
    }


    protected int queryPhotoMaxDimensions() {
        @Cleanup Cursor cursor = null;
        try {
            cursor = addressBook.provider.query(ContactsContract.DisplayPhoto.CONTENT_MAX_DIMENSIONS_URI,
                    new String[] { ContactsContract.DisplayPhoto.DISPLAY_MAX_DIM }, null, null, null);
            cursor.moveToFirst();
            return cursor.getInt(0);
        } catch(RemoteException e) {
            Constants.log.log(Level.SEVERE, "Couldn't get max photo dimensions, assuming 720x720 px", e);
            return 720;
        }
    }

    protected byte[] processPhoto(byte[] orig) {
        Constants.log.fine("Processing photo");
        Bitmap bitmap = BitmapFactory.decodeByteArray(orig, 0, orig.length);
        if (bitmap == null) {
            Constants.log.warning("Image decoding failed");
            return null;
        }

        int width = bitmap.getWidth(),
            height = bitmap.getHeight(),
            max = getPhotoMaxDimensions();

        if (width > max || height > max) {
            float   scaleWidth = (float)max/width,
                    scaleHeight = (float)max/height,
                    scale = Math.min(scaleWidth, scaleHeight);
            int     newWidth = (int)(width * scale),
                    newHeight = (int)(height * scale);

            Constants.log.fine("Resizing image from " + width + "x" + height + " to " + newWidth + "x" + newHeight);
            bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 97, baos)) {
                Constants.log.warning("Couldn't generate JPEG image");
                return orig;
            }
            return baos.toByteArray();
        }

        return orig;
    }


    protected void assertID() {
        if (id == null)
            throw new IllegalStateException("Contact has not been saved yet");
    }

    protected Uri rawContactSyncURI() {
        assertID();
        if (id == null)
            throw new IllegalStateException("Contact hasn't been saved yet");
        return addressBook.syncAdapterURI(ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, id));
    }

    protected Uri dataSyncURI() {
        return addressBook.syncAdapterURI(ContactsContract.Data.CONTENT_URI);
    }

}

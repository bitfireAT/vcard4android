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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
import ezvcard.util.IOUtils;
import lombok.Cleanup;
import lombok.Getter;
import lombok.NonNull;

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
                        Constants.log.error("Ignoring raw contact data row without " + ContactsContract.RawContactsEntity.MIMETYPE);
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
						case GroupMembership.CONTENT_ITEM_TYPE:
							populateGroupMembership(values);
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
        Integer type = row.getAsInteger(Phone.TYPE);
		if (type != null)
			switch (type) {
				case Phone.TYPE_HOME:
					number.addType(TelephoneType.HOME);
					break;
				case Phone.TYPE_MOBILE:
					number.addType(TelephoneType.CELL);
					break;
				case Phone.TYPE_WORK:
					number.addType(TelephoneType.WORK);
					break;
				case Phone.TYPE_FAX_WORK:
					number.addType(TelephoneType.FAX);
					number.addType(TelephoneType.WORK);
					break;
				case Phone.TYPE_FAX_HOME:
					number.addType(TelephoneType.FAX);
					number.addType(TelephoneType.HOME);
					break;
				case Phone.TYPE_PAGER:
					number.addType(TelephoneType.PAGER);
					break;
				case Phone.TYPE_CALLBACK:
					number.addType(Contact.PHONE_TYPE_CALLBACK);
					break;
				case Phone.TYPE_CAR:
					number.addType(TelephoneType.CAR);
					break;
				case Phone.TYPE_COMPANY_MAIN:
					number.addType(Contact.PHONE_TYPE_COMPANY_MAIN);
					break;
				case Phone.TYPE_ISDN:
					number.addType(TelephoneType.ISDN);
					break;
				case Phone.TYPE_MAIN:
					number.addType(TelephoneType.VOICE);
					break;
				case Phone.TYPE_OTHER_FAX:
					number.addType(TelephoneType.FAX);
					break;
				case Phone.TYPE_RADIO:
					number.addType(Contact.PHONE_TYPE_RADIO);
					break;
				case Phone.TYPE_TELEX:
					number.addType(TelephoneType.TEXTPHONE);
					break;
				case Phone.TYPE_TTY_TDD:
					number.addType(TelephoneType.TEXT);
					break;
				case Phone.TYPE_WORK_MOBILE:
					number.addType(TelephoneType.CELL);
					number.addType(TelephoneType.WORK);
					break;
				case Phone.TYPE_WORK_PAGER:
					number.addType(TelephoneType.PAGER);
					number.addType(TelephoneType.WORK);
					break;
				case Phone.TYPE_ASSISTANT:
					number.addType(Contact.PHONE_TYPE_ASSISTANT);
					break;
				case Phone.TYPE_MMS:
					number.addType(Contact.PHONE_TYPE_MMS);
					break;
				case Phone.TYPE_CUSTOM:
					String customType = row.getAsString(CommonDataKinds.Phone.LABEL);
					if (!TextUtils.isEmpty(customType))
						number.addType(TelephoneType.get(labelToXName(customType)));
			}
		if (row.getAsInteger(CommonDataKinds.Phone.IS_PRIMARY) != 0)
			number.setPref(1);

		contact.getPhoneNumbers().add(number);
	}

	protected void populateEmail(ContentValues row) {
		ezvcard.property.Email email = new ezvcard.property.Email(row.getAsString(Email.ADDRESS));
        Integer type = row.getAsInteger(Email.TYPE);
        if (type != null)
			switch (type) {
				case Email.TYPE_HOME:
					email.addType(EmailType.HOME);
					break;
				case Email.TYPE_WORK:
					email.addType(EmailType.WORK);
					break;
				case Email.TYPE_MOBILE:
					email.addType(Contact.EMAIL_TYPE_MOBILE);
					break;
				case Email.TYPE_CUSTOM:
					String customType = row.getAsString(Email.LABEL);
					if (!TextUtils.isEmpty(customType))
						email.addType(EmailType.get(labelToXName(customType)));
			}
		if (row.getAsInteger(Email.IS_PRIMARY) != 0)
			email.setPref(1);
		contact.getEmails().add(email);
	}

    protected void populatePhoto(ContentValues row) throws RemoteException {
        if (row.containsKey(Photo.PHOTO_FILE_ID)) {
            Uri photoUri = Uri.withAppendedPath(
                    rawContactSyncURI(),
                    RawContacts.DisplayPhoto.CONTENT_DIRECTORY);
            try {
                @Cleanup AssetFileDescriptor fd = addressBook.provider.openAssetFile(photoUri, "r");
                @Cleanup InputStream stream = fd.createInputStream();
                if (stream != null)
                    contact.photo = IOUtils.toByteArray(stream);
                else
                    Constants.log.warn("Ignoring inaccessible local contact photo file");
            } catch(IOException e) {
                Constants.log.warn("Couldn't read local contact photo file", e);
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
                org.addValue(company);
            if (!TextUtils.isEmpty(department))
                org.addValue(department);
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
            Constants.log.warn("Ignoring instant messenger record without handle");
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
                        Constants.log.error("Messenger type/value can't be expressed as URI; ignoring");
                    }
            }

        if (impp != null) {
            Integer type = row.getAsInteger(Im.TYPE);
            if (type != null)
                switch (type) {
                    case Im.TYPE_HOME:
                        impp.addType(ImppType.HOME);
                        break;
                    case Im.TYPE_WORK:
                        impp.addType(ImppType.WORK);
                        break;
                    case Im.TYPE_CUSTOM:
                        String customType = row.getAsString(Im.LABEL);
                        if (!TextUtils.isEmpty(customType))
                            impp.addType(ImppType.get(labelToXName(customType)));
                }

            contact.getImpps().add(impp);
        }
    }

    protected void populateNickname(ContentValues row) {
        if (row.containsKey(Nickname.NAME)) {
            ezvcard.property.Nickname nick = new ezvcard.property.Nickname();

            nick.addValue(row.getAsString(Nickname.NAME));

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
        address.setLabel(row.getAsString(StructuredPostal.FORMATTED_ADDRESS));
        if (row.containsKey(StructuredPostal.TYPE))
            switch (row.getAsInteger(StructuredPostal.TYPE)) {
                case StructuredPostal.TYPE_HOME:
                    address.addType(AddressType.HOME);
                    break;
                case StructuredPostal.TYPE_WORK:
                    address.addType(AddressType.WORK);
                    break;
                case StructuredPostal.TYPE_CUSTOM:
                    String customType = row.getAsString(StructuredPostal.LABEL);
                    if (!TextUtils.isEmpty(customType))
                        address.addType(AddressType.get(labelToXName(customType)));
                    break;
            }
        address.setStreetAddress(row.getAsString(StructuredPostal.STREET));
        address.setPoBox(row.getAsString(StructuredPostal.POBOX));
        address.setExtendedAddress(row.getAsString(StructuredPostal.NEIGHBORHOOD));
        address.setLocality(row.getAsString(StructuredPostal.CITY));
        address.setRegion(row.getAsString(StructuredPostal.REGION));
        address.setPostalCode(row.getAsString(StructuredPostal.POSTCODE));
        address.setCountry(row.getAsString(StructuredPostal.COUNTRY));
        contact.getAddresses().add(address);
    }

    protected void populateGroupMembership(ContentValues row) {
        // vcard4android doesn't have group support by default.
        // Override this method to read local group memberships.
    }

    protected void populateWebsite(ContentValues row) {
        Url url = new Url(row.getAsString(Website.URL));
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
                    if (!TextUtils.isEmpty(label))
                        url.setType(labelToXName(label));
                    break;
            }
        contact.getURLs().add(url);
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
            Constants.log.warn("Couldn't parse birthday/anniversary date from database", e);
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
                    related.addType(RelatedType.CO_WORKER);
                    break;
                case Relation.TYPE_BROTHER:
                case Relation.TYPE_SISTER:
                    related.addType(RelatedType.SIBLING);
                    break;
                case Relation.TYPE_CHILD:
                    related.addType(RelatedType.CHILD);
                    break;
                case Relation.TYPE_DOMESTIC_PARTNER:
                    related.addType(RelatedType.CO_RESIDENT);
                    break;
                case Relation.TYPE_FRIEND:
                    related.addType(RelatedType.FRIEND);
                    break;
                case Relation.TYPE_FATHER:
                case Relation.TYPE_MOTHER:
                case Relation.TYPE_PARENT:
                    related.addType(RelatedType.PARENT);
                    break;
                case Relation.TYPE_PARTNER:
                case Relation.TYPE_SPOUSE:
                    related.addType(RelatedType.SWEETHEART);
                    break;
                case Relation.TYPE_RELATIVE:
                    related.addType(RelatedType.KIN);
                    break;
            }

        contact.getRelations().add(related);
    }

    protected void populateSipAddress(ContentValues row) {
        try {
            Impp impp = new Impp("sip:" + row.getAsString(SipAddress.SIP_ADDRESS));
            if (row.containsKey(SipAddress.TYPE))
                switch (row.getAsInteger(SipAddress.TYPE)) {
                    case SipAddress.TYPE_HOME:
                        impp.addType(ImppType.HOME);
                        break;
                    case SipAddress.TYPE_WORK:
                        impp.addType(ImppType.WORK);
                        break;
                    case SipAddress.TYPE_CUSTOM:
                        String customType = row.getAsString(SipAddress.LABEL);
                        if (!TextUtils.isEmpty(customType))
                            impp.addType(ImppType.get(labelToXName(customType)));
                }
            contact.getImpps().add(impp);
        } catch(IllegalArgumentException e) {
            Constants.log.warn("Ignoring invalid locally stored SIP address");
        }
    }


    public Uri add() throws ContactsStorageException {
		BatchOperation batch = new BatchOperation(addressBook.provider);

		ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(addressBook.syncAdapterURI(RawContacts.CONTENT_URI));
		buildContact(builder, false);
		batch.enqueue(builder.build());

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
        batch.enqueue(builder.build());

        // delete old data rows before adding the new ones
        Uri dataRowsUri = addressBook.syncAdapterURI(ContactsContract.Data.CONTENT_URI);
        batch.enqueue(ContentProviderOperation.newDelete(dataRowsUri)
                .withSelection(RawContacts.Data.RAW_CONTACT_ID + "=?", new String[] { String.valueOf(id) })
                .build());
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
	}


	protected void insertDataRows(BatchOperation batch) throws ContactsStorageException {
		insertStructuredName(batch);

		for (Telephone number : contact.getPhoneNumbers())
			insertPhoneNumber(batch, number);

		for (ezvcard.property.Email email : contact.getEmails())
			insertEmail(batch, email);

        insertOrganization(batch);

        for (Impp impp : contact.getImpps())        // handles SIP addresses, too
            insertIMPP(batch, impp);

        insertNickname(batch);

        insertNote(batch);

        for (Address address : contact.getAddresses())
            insertStructuredPostal(batch, address);

        insertGroupMemberships(batch);

        for (Url url : contact.getURLs())
            insertWebsite(batch, url);

        if (contact.anniversary != null)
            insertEvent(batch, Event.TYPE_ANNIVERSARY, contact.anniversary);
        if (contact.birthDay != null)
            insertEvent(batch, Event.TYPE_BIRTHDAY, contact.birthDay);

        for (Related related : contact.getRelations())
            insertRelation(batch, related);
	}

	protected void insertStructuredName(BatchOperation batch) {
		ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(dataSyncURI());
        if (id == null)
            builder.withValueBackReference(StructuredName.RAW_CONTACT_ID, 0);
        else
            builder.withValue(StructuredName.RAW_CONTACT_ID, id);
		builder .withValue(RawContacts.Data.MIMETYPE, CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
				.withValue(StructuredName.PREFIX, contact.prefix)
				.withValue(StructuredName.DISPLAY_NAME, contact.displayName)
				.withValue(StructuredName.GIVEN_NAME, contact.givenName)
				.withValue(StructuredName.MIDDLE_NAME, contact.middleName)
				.withValue(StructuredName.FAMILY_NAME, contact.familyName)
				.withValue(StructuredName.SUFFIX, contact.suffix)
				.withValue(StructuredName.PHONETIC_GIVEN_NAME, contact.phoneticGivenName)
				.withValue(StructuredName.PHONETIC_MIDDLE_NAME, contact.phoneticMiddleName)
				.withValue(StructuredName.PHONETIC_FAMILY_NAME, contact.phoneticFamilyName);
		batch.enqueue(builder.build());
	}

	protected void insertPhoneNumber(BatchOperation batch, Telephone number) {
		int typeCode = Phone.TYPE_OTHER;
		String typeLabel = null;

		Set<TelephoneType> types = number.getTypes();

		// preferred number?
		boolean is_primary = number.getPref() != null;
		if (types.contains(TelephoneType.PREF)) {
			is_primary = true;
			types.remove(TelephoneType.PREF);
		}

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

        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(dataSyncURI());
        if (id == null)
            builder.withValueBackReference(Phone.RAW_CONTACT_ID, 0);
        else
            builder.withValue(Phone.RAW_CONTACT_ID, id);
		builder	.withValue(Phone.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
				.withValue(Phone.NUMBER, number.getText())
				.withValue(Phone.TYPE, typeCode)
                .withValue(Phone.LABEL, typeLabel)
				.withValue(Phone.IS_PRIMARY, is_primary ? 1 : 0)
				.withValue(Phone.IS_SUPER_PRIMARY, is_primary ? 1 : 0);
		batch.enqueue(builder.build());
	}

	protected void insertEmail(BatchOperation batch, ezvcard.property.Email email) {
		int typeCode = 0;
		String typeLabel = null;

		Set<EmailType> types = email.getTypes();

		// preferred email address?
		boolean is_primary = email.getPref() != null;
		if (types.contains(EmailType.PREF)) {
			is_primary = true;
			types.remove(EmailType.PREF);
		}

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

        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(dataSyncURI());
        if (id == null)
            builder.withValueBackReference(Email.RAW_CONTACT_ID, 0);
        else
            builder.withValue(Email.RAW_CONTACT_ID, id);
		builder	.withValue(Email.MIMETYPE, Email.CONTENT_ITEM_TYPE)
				.withValue(Email.ADDRESS, email.getValue())
				.withValue(Email.TYPE, typeCode)
                .withValue(Email.LABEL, typeLabel)
				.withValue(Email.IS_PRIMARY, is_primary ? 1 : 0)
				.withValue(Phone.IS_SUPER_PRIMARY, is_primary ? 1 : 0);
        batch.enqueue(builder.build());
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

        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(dataSyncURI());
        if (id == null)
            builder.withValueBackReference(Organization.RAW_CONTACT_ID, 0);
        else
            builder.withValue(Organization.RAW_CONTACT_ID, id);
        builder .withValue(Organization.MIMETYPE, Organization.CONTENT_ITEM_TYPE)
                .withValue(Organization.COMPANY, company)
                .withValue(Organization.DEPARTMENT, department)
                .withValue(Organization.TITLE, contact.jobTitle)
                .withValue(Organization.JOB_DESCRIPTION, contact.jobDescription);
        batch.enqueue(builder.build());
    }

    protected void insertIMPP(BatchOperation batch, Impp impp) {
        int typeCode = Im.TYPE_OTHER;       // default value
        String typeLabel = null;

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

        String protocol = impp.getProtocol();
        if (protocol == null) {
            Constants.log.warn("Ignoring IMPP address without protocol");
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

        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(dataSyncURI());
        if (id == null)
            builder.withValueBackReference(Im.RAW_CONTACT_ID, 0);
        else
            builder.withValue(Im.RAW_CONTACT_ID, id);
        if (sipAddress)
            // save as SIP address
            builder	.withValue(SipAddress.MIMETYPE, SipAddress.CONTENT_ITEM_TYPE)
                    .withValue(SipAddress.DATA, impp.getHandle())
                    .withValue(SipAddress.TYPE, typeCode)
                    .withValue(SipAddress.LABEL, typeLabel);
        else {
            // save as IM address
            builder	.withValue(Im.MIMETYPE, Im.CONTENT_ITEM_TYPE)
                    .withValue(Im.DATA, impp.getHandle())
                    .withValue(Im.TYPE, typeCode)
                    .withValue(Im.LABEL, typeLabel)
                    .withValue(Im.PROTOCOL, protocolCode)
                    .withValue(Im.CUSTOM_PROTOCOL, protocolLabel);
        }
        batch.enqueue(builder.build());
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

            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(dataSyncURI());
            if (id == null)
                builder.withValueBackReference(Nickname.RAW_CONTACT_ID, 0);
            else
                builder.withValue(Nickname.RAW_CONTACT_ID, id);
            builder .withValue(Nickname.MIMETYPE, Nickname.CONTENT_ITEM_TYPE)
                    .withValue(Nickname.NAME, nick.getValues().get(0))
                    .withValue(Nickname.TYPE, typeCode)
                    .withValue(Nickname.LABEL, typeLabel);
            batch.enqueue(builder.build());
        }
    }

    protected void insertNote(BatchOperation batch) {
        if (!TextUtils.isEmpty(contact.note)) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(dataSyncURI());
            if (id == null)
                builder.withValueBackReference(Note.RAW_CONTACT_ID, 0);
            else
                builder.withValue(Note.RAW_CONTACT_ID, id);
            builder .withValue(Note.MIMETYPE, Note.CONTENT_ITEM_TYPE)
                    .withValue(Note.NOTE, contact.note);
            batch.enqueue(builder.build());
        }
    }

    protected void insertStructuredPostal(BatchOperation batch, Address address) {
		/*	street po.box (extended)
		 *	postcode city
		 *	region
		 *	COUNTRY
		 */
        String formattedAddress = address.getLabel();
        if (!TextUtils.isEmpty(formattedAddress)) {
            String  lineStreet = StringUtils.join(new String[] { address.getStreetAddress(), address.getPoBox(), address.getExtendedAddress() }, " "),
                    lineLocality = StringUtils.join(new String[]{address.getPostalCode(), address.getLocality()}, " ");

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

        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(dataSyncURI());
        if (id == null)
            builder.withValueBackReference(StructuredPostal.RAW_CONTACT_ID, 0);
        else
            builder.withValue(StructuredPostal.RAW_CONTACT_ID, id);
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
        batch.enqueue(builder.build());
    }

    protected void insertGroupMemberships(BatchOperation batch) throws ContactsStorageException {
        // vcard4android doesn't have group support by default.
        // Override this method to add/update local group memberships.
    }

    protected void insertWebsite(BatchOperation batch, Url url) {
        int typeCode = Website.TYPE_OTHER;
        String typeLabel = null;

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

        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(dataSyncURI());
        if (id == null)
            builder.withValueBackReference(Website.RAW_CONTACT_ID, 0);
        else
            builder.withValue(Website.RAW_CONTACT_ID, id);
        builder .withValue(Website.MIMETYPE, Website.CONTENT_ITEM_TYPE)
                .withValue(Website.URL, url.getValue())
                .withValue(Website.TYPE, typeCode)
                .withValue(Website.LABEL, typeLabel);
        batch.enqueue(builder.build());
    }

    protected void insertEvent(BatchOperation batch, int type, DateOrTimeProperty dateOrTime) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        if (dateOrTime.getDate() == null) {
            Constants.log.warn("Ignoring contact event (birthday/anniversary) without date");
            return;
        }

        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(dataSyncURI());
        if (id == null)
            builder.withValueBackReference(Event.RAW_CONTACT_ID, 0);
        else
            builder.withValue(Event.RAW_CONTACT_ID, id);
        builder .withValue(Event.MIMETYPE, Event.CONTENT_ITEM_TYPE)
                .withValue(Event.TYPE, type)
                .withValue(Event.START_DATE, formatter.format(dateOrTime.getDate()));
        batch.enqueue(builder.build());
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

        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(dataSyncURI());
        if (id == null)
            builder.withValueBackReference(Relation.RAW_CONTACT_ID, 0);
        else
            builder.withValue(Relation.RAW_CONTACT_ID, id);
        builder .withValue(Relation.MIMETYPE, Relation.CONTENT_ITEM_TYPE)
                .withValue(Relation.NAME, related.getText())
                .withValue(Relation.TYPE, typeCode)
                .withValue(Relation.LABEL, StringUtils.join(labels, "/"));
        batch.enqueue(builder.build());
    }

    protected void insertPhoto(byte[] photo) {
        if (photo != null) {
            // The following approach would be correct, but it doesn't work:
            // the ContactsProvider handler will process the image in background and update
            // the raw contact with the new photo ID when it's finished, setting it to dirty again!
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
            // a TransactionTooLargeException for photos > 1 MB
            Constants.log.debug("Inserting photo for raw contact {}", id);

            ContentValues values = new ContentValues(2);
            values.put(RawContacts.Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
            values.put(Photo.RAW_CONTACT_ID, id);
            values.put(Photo.PHOTO, photo);
            try {
                addressBook.provider.insert(dataSyncURI(), values);
            } catch (RemoteException e) {
                Constants.log.warn("Couldn't set local contact photo, ignoring", e);
            }
        }
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


    protected Uri rawContactSyncURI() {
        if (id == null)
            throw new IllegalStateException("Contact hasn't been saved yet");
        return addressBook.syncAdapterURI(ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, id));
    }

    protected Uri dataSyncURI() {
        return addressBook.syncAdapterURI(ContactsContract.Data.CONTENT_URI);
    }

}

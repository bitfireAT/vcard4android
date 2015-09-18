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
import android.net.Uri;
import android.os.RemoteException;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.*;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import ezvcard.parameter.EmailType;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.*;
import lombok.Cleanup;

public class AndroidContact {

	final protected AndroidAddressBook addressBook;

	protected Long id;
	private Contact contact;

	protected AndroidContact(AndroidAddressBook addressBook, long id) {
		this.addressBook = addressBook;
		this.id = id;
	}

	protected AndroidContact(AndroidAddressBook addressBook, Contact contact) {
		this.addressBook = addressBook;
		this.contact = contact;
	}

	public Contact getContact() throws FileNotFoundException, ContactsStorageException {
		if (contact != null)
			return contact;

		try {
			@Cleanup EntityIterator iter = ContactsContract.RawContacts.newEntityIterator(addressBook.provider.query(
					addressBook.syncAdapterURI(ContactsContract.RawContactsEntity.CONTENT_URI),
					null, ContactsContract.RawContacts._ID + "=" + id,
					null, null));

			if (iter.hasNext()) {
				Entity e = iter.next();

				contact = new Contact();
				populateContact(e.getEntityValues());

				List<Entity.NamedContentValues> subValues = e.getSubValues();
				for (Entity.NamedContentValues subValue : subValues) {
					ContentValues values = subValue.values;
					String mimeType = values.getAsString(ContactsContract.RawContactsEntity.MIMETYPE);
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
						/*case Photo.CONTENT_ITEM_TYPE:
							populatePhoto(c, values);
							break;
						case Organization.CONTENT_ITEM_TYPE:
							populateOrganization(c, values);
							break;
						case Im.CONTENT_ITEM_TYPE:
							populateIMPP(c, values);
							break;
						case Nickname.CONTENT_ITEM_TYPE:
							populateNickname(c, values);
							break;
						case Note.CONTENT_ITEM_TYPE:
							populateNote(c, values);
							break;
						case StructuredPostal.CONTENT_ITEM_TYPE:
							populatePostalAddress(c, values);
							break;
						case GroupMembership.CONTENT_ITEM_TYPE:
							populateGroupMembership(c, values);
							break;
						case Website.CONTENT_ITEM_TYPE:
							populateURL(c, values);
							break;
						case CommonDataKinds.Event.CONTENT_ITEM_TYPE:
							populateEvent(c, values);
							break;
						case Relation.CONTENT_ITEM_TYPE:
							populateRelation(c, values);
							break;
						case SipAddress.CONTENT_ITEM_TYPE:
							populateSipAddress(c, values);
							break;*/
					}
				}

				return contact;
			} else
				throw new FileNotFoundException();
		} catch(RemoteException e) {
			throw new ContactsStorageException("Couldn't read local contact", e);
		}
	}

	protected void populateContact(ContentValues values) {
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
		if (row.containsKey(Phone.TYPE))
			switch (row.getAsInteger(Phone.TYPE)) {
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
					if (StringUtils.isNotEmpty(customType))
						number.addType(TelephoneType.get(labelToXName(customType)));
			}
		if (row.getAsInteger(CommonDataKinds.Phone.IS_PRIMARY) != 0)
			number.setPref(1);

		contact.getPhoneNumbers().add(number);
	}

	protected void populateEmail(ContentValues row) {
		ezvcard.property.Email email = new ezvcard.property.Email(row.getAsString(Email.ADDRESS));
		if (row.containsKey(Email.TYPE))
			switch (row.getAsInteger(Email.TYPE)) {
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
					if (StringUtils.isNotEmpty(customType))
						email.addType(EmailType.get(labelToXName(customType)));
			}
		if (row.getAsInteger(Email.IS_PRIMARY) != 0)
			email.setPref(1);
		contact.getEmails().add(email);
	}


	public Uri add() throws ContactsStorageException {
		BatchOperation batch = new BatchOperation(addressBook.provider);

		ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(addressBook.syncAdapterURI(ContactsContract.RawContacts.CONTENT_URI));
		buildContact(builder, false);
		batch.enqueue(builder.build());

		addDataRows(batch, false);

		batch.commit();
		Uri uri = batch.getResult(0).uri;
		id = ContentUris.parseId(uri);
		return uri;
	}

	public int delete() throws ContactsStorageException {
		try {
			return addressBook.provider.delete(addressBook.syncAdapterURI(ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, id)), null, null);
		} catch (RemoteException e) {
			throw new ContactsStorageException("Couldn't delete local contact", e);
		}
	}

	protected void buildContact(ContentProviderOperation.Builder builder, boolean update) {
		if (!update)
			builder	.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, addressBook.account.name)
					.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, addressBook.account.type);
	}


	protected void addDataRows(BatchOperation batch, boolean update) {
		addStructuredName(batch);
		for (Telephone number : contact.getPhoneNumbers())
			addPhoneNumber(batch, number);
		for (ezvcard.property.Email email : contact.getEmails())
			addEmail(batch, email);
	}

	protected void addStructuredName(BatchOperation batch) {
		ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
		builder .withValueBackReference(StructuredName.RAW_CONTACT_ID, 0)
				.withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
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

	protected void addPhoneNumber(BatchOperation batch, Telephone number) {
		ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
		builder .withValueBackReference(CommonDataKinds.Phone.RAW_CONTACT_ID, 0)
				.withValue(ContactsContract.Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);

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

		builder	.withValue(ContactsContract.Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
				.withValue(Phone.NUMBER, number.getText())
				.withValue(Phone.TYPE, typeCode)
				.withValue(Phone.IS_PRIMARY, is_primary ? 1 : 0)
				.withValue(Phone.IS_SUPER_PRIMARY, is_primary ? 1 : 0);
		if (typeLabel != null)
			builder.withValue(Phone.LABEL, typeLabel);

		batch.enqueue(builder.build());
	}

	protected void addEmail(BatchOperation batch, ezvcard.property.Email email) {
		ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
		builder .withValueBackReference(CommonDataKinds.Email.RAW_CONTACT_ID, 0)
				.withValue(ContactsContract.Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);

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

		builder	.withValue(ContactsContract.Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
				.withValue(Email.ADDRESS, email.getValue())
				.withValue(Email.TYPE, typeCode)
				.withValue(Email.IS_PRIMARY, is_primary ? 1 : 0)
				.withValue(Phone.IS_SUPER_PRIMARY, is_primary ? 1 : 0);
		if (typeLabel != null)
			builder.withValue(Email.LABEL, typeLabel);
	}


	protected static String labelToXName(String label) {
		return "X-" + label.replaceAll(" ","_").replaceAll("[^\\p{L}\\p{Nd}\\-_]", "").toUpperCase(Locale.US);
	}

	protected static String xNameToLabel(String xname) {
		// "X-MY_PROPERTY"
		// 1. ensure lower case -> "x-my_property"
		// 2. remove x- from beginning -> "my_property"
		// 3. replace "_" by " " -> "my property"
		// 4. capitalize -> "My Property"
		String	lowerCase = StringUtils.lowerCase(xname, Locale.US),
				withoutPrefix = StringUtils.removeStart(lowerCase, "x-"),
				withSpaces = StringUtils.replace(withoutPrefix, "_", " ");
		return WordUtils.capitalize(withSpaces);
	}

}

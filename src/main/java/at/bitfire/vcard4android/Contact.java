/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.vcard4android;

import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.VCardVersion;
import ezvcard.ValidationWarnings;
import ezvcard.Warning;
import ezvcard.parameter.EmailType;
import ezvcard.parameter.ImageType;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.Address;
import ezvcard.property.Anniversary;
import ezvcard.property.Birthday;
import ezvcard.property.Categories;
import ezvcard.property.Email;
import ezvcard.property.FormattedName;
import ezvcard.property.Impp;
import ezvcard.property.Logo;
import ezvcard.property.Nickname;
import ezvcard.property.Note;
import ezvcard.property.Organization;
import ezvcard.property.Photo;
import ezvcard.property.ProductId;
import ezvcard.property.RawProperty;
import ezvcard.property.Related;
import ezvcard.property.Revision;
import ezvcard.property.Role;
import ezvcard.property.Sound;
import ezvcard.property.Source;
import ezvcard.property.StructuredName;
import ezvcard.property.Telephone;
import ezvcard.property.Title;
import ezvcard.property.Uid;
import ezvcard.property.Url;
import ezvcard.property.VCardProperty;
import lombok.Cleanup;
import lombok.Getter;
import lombok.NonNull;

public class Contact {
	private static final String TAG = "vcard4android.Contact";

    // productID (if set) will be used to generate a PRODID property.
    // You may set this statically from the calling application.
    public static String productID = null;

	public static final String
			PROPERTY_PHONETIC_FIRST_NAME = "X-PHONETIC-FIRST-NAME",
			PROPERTY_PHONETIC_MIDDLE_NAME = "X-PHONETIC-MIDDLE-NAME",
			PROPERTY_PHONETIC_LAST_NAME = "X-PHONETIC-LAST-NAME",
			PROPERTY_SIP = "X-SIP";

	public static final TelephoneType
			PHONE_TYPE_CALLBACK = TelephoneType.get("x-callback"),
			PHONE_TYPE_COMPANY_MAIN = TelephoneType.get("x-company_main"),
			PHONE_TYPE_RADIO = TelephoneType.get("x-radio"),
			PHONE_TYPE_ASSISTANT = TelephoneType.get("X-assistant"),
			PHONE_TYPE_MMS = TelephoneType.get("x-mms");

	public static final EmailType EMAIL_TYPE_MOBILE = EmailType.get("x-mobile");

    public static final String
            NICKNAME_TYPE_MAIDEN_NAME = "x-maiden-name",
            NICKNAME_TYPE_SHORT_NAME = "x-short-name",
            NICKNAME_TYPE_INITIALS = "x-initials",
            NICKNAME_TYPE_OTHER_NAME = "x-other-name";

    public static final String
            URL_TYPE_HOMEPAGE = "x-homepage",
            URL_TYPE_BLOG = "x-blog",
            URL_TYPE_PROFILE = "x-profile",
            URL_TYPE_FTP = "x-ftp";

    public String uid;
	public String displayName;
	public String prefix, givenName, middleName, familyName, suffix;
	public String phoneticGivenName, phoneticMiddleName, phoneticFamilyName;
    public Nickname nickName;

    public Organization organization;
    public String   jobTitle,           // VCard TITLE
                    jobDescription;     // VCard ROLE

	@Getter private List<Telephone> phoneNumbers = new LinkedList<>();
	@Getter private List<Email> emails = new LinkedList<>();
    @Getter private List<Impp> impps = new LinkedList<>();
    @Getter private List<Address> addresses = new LinkedList<>();
    @Getter private List<String> categories = new LinkedList<>();
    @Getter private List<Url> URLs = new LinkedList<>();
    @Getter private List<Related> relations = new LinkedList<>();

    public String note;

    public Anniversary anniversary;
    public Birthday birthDay;

    public byte[] photo;

    // unknown properties in text VCARD format
    public String unknownProperties;



    /**
	 * Parses an InputStream that contains a VCard.
	 *
	 * @param stream     input stream containing the VCard (any parsable version, i.e. 3 or 4)
	 * @param charset    charset of the input stream or null (will assume UTF-8)
     * @param downloader will be used to download external resources like contact photos (may be null)
	 * @return array of filled Event data objects (may have size 0) – doesn't return null
	 */
	public static Contact[] fromStream(@NonNull InputStream stream, Charset charset, Downloader downloader) throws IOException {
		final List<VCard> vcards;
		if (charset != null) {
			@Cleanup InputStreamReader reader = new InputStreamReader(stream, charset);
			vcards = Ezvcard.parse(reader).all();
		} else
			vcards = Ezvcard.parse(stream).all();

		List<Contact> contacts = new LinkedList<>();
		for (VCard vcard : vcards)
			contacts.add(fromVCard(vcard, downloader));
		return contacts.toArray(new Contact[contacts.size()]);
	}


	protected static Contact fromVCard(VCard vCard, Downloader downloader) {
		Contact c = new Contact();

		// UID
		Uid uid = vCard.getUid();
		if (uid != null) {
			c.uid = uid.getValue();
			vCard.removeProperties(Uid.class);
		} else {
			Constants.log.warn("Received VCard without UID, generating new one");
			c.generateUID();
		}

		// FN
		FormattedName fn = vCard.getFormattedName();
		if (fn != null) {
			c.displayName = fn.getValue();
			vCard.removeProperties(FormattedName.class);
		} else
            Constants.log.warn("Received VCard without FN (formatted name)");

		// N
		StructuredName n = vCard.getStructuredName();
		if (n != null) {
			c.prefix = TextUtils.join(" ", n.getPrefixes());
			c.givenName = n.getGiven();
			c.middleName = TextUtils.join(" ", n.getAdditional());
			c.familyName = n.getFamily();
			c.suffix = TextUtils.join(" ", n.getSuffixes());
			vCard.removeProperties(StructuredName.class);
		} else
            Constants.log.warn("Received VCard without N (structured name)");

		// phonetic names
		RawProperty phoneticFirstName = vCard.getExtendedProperty(PROPERTY_PHONETIC_FIRST_NAME),
					phoneticMiddleName = vCard.getExtendedProperty(PROPERTY_PHONETIC_MIDDLE_NAME),
					phoneticLastName = vCard.getExtendedProperty(PROPERTY_PHONETIC_LAST_NAME);
		if (phoneticFirstName != null) {
			c.phoneticGivenName = phoneticFirstName.getValue();
			vCard.removeExtendedProperty(PROPERTY_PHONETIC_FIRST_NAME);
		}
		if (phoneticMiddleName != null) {
			c.phoneticMiddleName = phoneticMiddleName.getValue();
			vCard.removeExtendedProperty(PROPERTY_PHONETIC_MIDDLE_NAME);
		}
		if (phoneticLastName != null) {
			c.phoneticFamilyName = phoneticLastName.getValue();
			vCard.removeExtendedProperty(PROPERTY_PHONETIC_LAST_NAME);
		}

		// TEL
		c.phoneNumbers = vCard.getTelephoneNumbers();
		vCard.removeProperties(Telephone.class);

		// EMAIL
		c.emails = vCard.getEmails();
		vCard.removeProperties(Email.class);

        // ORG
        c.organization = vCard.getOrganization();
        vCard.removeProperties(Organization.class);
        // TITLE
        for (Title title : vCard.getTitles()) {
            c.jobTitle = title.getValue();
            vCard.removeProperties(Title.class);
            break;
        }
        // ROLE
        for (Role role : vCard.getRoles()) {
            c.jobDescription = role.getValue();
            vCard.removeProperties(Role.class);
            break;
        }

        // IMPP
        c.impps = vCard.getImpps();
        vCard.removeProperties(Impp.class);
        // add X-SIP properties as IMPP, too
        for (RawProperty sip : vCard.getExtendedProperties(PROPERTY_SIP))
            c.impps.add(new Impp("sip", sip.getValue()));
        vCard.removeExtendedProperty(PROPERTY_SIP);

        // NICKNAME
        c.nickName = vCard.getNickname();
        vCard.removeProperties(Nickname.class);

        // ADR
        c.addresses = vCard.getAddresses();
        vCard.removeProperties(Address.class);

        // NOTE
        List<String> notes = new LinkedList<>();
        for (Note note : vCard.getNotes())
            notes.add(note.getValue());
        if (!notes.isEmpty())
            c.note = TextUtils.join("\n\n\n", notes);
        vCard.removeProperties(Note.class);

        // CATEGORY
        Categories categories = vCard.getCategories();
        if (categories != null)
            c.categories = categories.getValues();
        vCard.removeProperties(Categories.class);

        // URL
        c.URLs = vCard.getUrls();
        vCard.removeProperties(Url.class);

        // BDAY
        c.birthDay = vCard.getBirthday();
        vCard.removeProperties(Birthday.class);
        // ANNIVERSARY
        c.anniversary = vCard.getAnniversary();
        vCard.removeProperties(Anniversary.class);

        // RELATED
        for (Related related : vCard.getRelations()) {
            String text = related.getText();
            if (!TextUtils.isEmpty(text)) {
                // process only free-form relations with text
                c.relations.add(related);
                vCard.removeProperty(related);
            }
        }

        // PHOTO
        for (Photo photo : vCard.getPhotos()) {
            c.photo = photo.getData();
            if (c.photo == null && photo.getUrl() != null) {
                String url = photo.getUrl();
                Constants.log.info("Downloading photo from " + url);
                c.photo = downloader.download(url, "image/*");
            }
            if (c.photo != null)
                break;
        }
        vCard.removeProperties(Photo.class);

        // remove binary properties because of potential OutOfMemory / TransactionTooLarge exceptions
        vCard.removeProperties(Logo.class);
        vCard.removeProperties(Sound.class);
        // remove properties that don't apply anymore
        vCard.removeProperties(ProductId.class);
        vCard.removeProperties(Revision.class);
        vCard.removeProperties(Source.class);
        // store all remaining properties into unknownProperties
        if (!vCard.getProperties().isEmpty() || !vCard.getExtendedProperties().isEmpty())
            try {
                c.unknownProperties = vCard.write();
            } catch(Exception e) {
                Log.w(TAG, "Couldn't serialize unknown properties, dropping them");
            }

        return c;
	}

    public ByteArrayOutputStream toStream(VCardVersion vCardVersion) throws IOException {
        VCard vCard = null;
        try {
            if (unknownProperties != null)
                vCard = Ezvcard.parse(unknownProperties).first();
        } catch (Exception e) {
            Log.e(TAG, "Couldn't parse unknown original properties, creating from scratch");
        }
        if (vCard == null)
            vCard = new VCard();

        // UID
        if (uid != null)
            vCard.setUid(new Uid(uid));
        else
            Log.wtf(TAG, "Generating VCard without UID");

        // PRODID
        if (productID != null)
            vCard.setProductId(productID);

        // FN
        String fn = null;
        if (displayName != null)
            fn = displayName;
        else if (organization != null && organization.getValues() != null && organization.getValues().get(0) != null)
            fn = organization.getValues().get(0);
        else if (nickName != null)
            fn = nickName.getValues().get(0);
        else {
            if (!phoneNumbers.isEmpty())
                fn = phoneNumbers.get(0).getText();
            else if (!emails.isEmpty())
                fn = emails.get(0).getValue();
            Constants.log.warn("No FN (formatted name) available, using " + fn);
        }
        if (TextUtils.isEmpty(fn)) {
            fn = "-";
            Constants.log.warn("No FN (formatted name) available, using \"-\"");
        }
        vCard.setFormattedName(fn);

        // N
        StructuredName n = new StructuredName();
        if (prefix != null || familyName != null || middleName != null || givenName != null || suffix != null) {
            if (prefix != null)
                for (String p : TextUtils.split(prefix, " "))
                    n.addPrefix(p);
            n.setGiven(givenName);
            if (middleName != null)
                for (String middle : TextUtils.split(middleName, " "))
                    n.addAdditional(middle);
            n.setFamily(familyName);
            if (suffix != null)
                for (String s : TextUtils.split(suffix, " "))
                    n.addSuffix(s);
        } else {
            n.setGiven(fn);
            Constants.log.warn("No N (structured name) available, using first name \"" + fn + "\"");
        }
        vCard.setStructuredName(n);

        // phonetic names
        if (phoneticGivenName != null)
            vCard.addExtendedProperty(PROPERTY_PHONETIC_FIRST_NAME, phoneticGivenName);
        if (phoneticMiddleName != null)
            vCard.addExtendedProperty(PROPERTY_PHONETIC_MIDDLE_NAME, phoneticMiddleName);
        if (phoneticFamilyName != null)
            vCard.addExtendedProperty(PROPERTY_PHONETIC_LAST_NAME, phoneticFamilyName);

        // TEL
        for (Telephone phoneNumber : phoneNumbers)
            vCard.addTelephoneNumber(phoneNumber);

        // EMAIL
        for (Email email : emails)
            vCard.addEmail(email);

        // ORG, TITLE, ROLE
        if (organization != null)
            vCard.setOrganization(organization);
        if (jobTitle != null)
            vCard.addTitle(jobTitle);
        if (jobDescription != null)
            vCard.addRole(jobDescription);

        // IMPP
        for (Impp impp : impps)
            vCard.addImpp(impp);

        // NICKNAME
        if (nickName != null)
            vCard.setNickname(nickName);

        // ADR
        for (Address address : addresses)
            vCard.addAddress(address);

        // NOTE
        if (note != null)
            vCard.addNote(note);

        // CATEGORIES
        if (!categories.isEmpty())
            vCard.setCategories(categories.toArray(new String[categories.size()]));

        // URL
        for (Url url : URLs)
            vCard.addUrl(url);

        // ANNIVERSARY
        if (anniversary != null)
            vCard.setAnniversary(anniversary);
        // BDAY
        if (birthDay != null)
            vCard.setBirthday(birthDay);

        // RELATED
        for (Related related : relations)
            vCard.addRelated(related);

        // PHOTO
        if (photo != null)
            vCard.addPhoto(new Photo(photo, ImageType.JPEG));

        // REV
        vCard.setRevision(Revision.now());

        // validate VCard and log results
        ValidationWarnings validation = vCard.validate(vCardVersion);
        if (!validation.isEmpty()) {
            Constants.log.warn("Generating possibly invalid VCard:");
            for (Map.Entry<VCardProperty, List<Warning>> entry : validation)
                for (Warning warning : entry.getValue())
                    Constants.log.warn("  * " + entry.getKey().getClass().getSimpleName() + " - " + warning.getMessage());
        }

        // generate VCARD
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Ezvcard .write(vCard)
                .version(vCardVersion)
                .versionStrict(false)
                .prodId(productID == null)
                .go(os);
        return os;
    }


	protected void generateUID() {
		uid = UUID.randomUUID().toString();
	}


    public interface Downloader {
        byte[] download(String url, String accepts);
    }

}

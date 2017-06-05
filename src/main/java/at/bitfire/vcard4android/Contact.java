/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.vcard4android;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.VCardVersion;
import ezvcard.ValidationWarning;
import ezvcard.ValidationWarnings;
import ezvcard.parameter.EmailType;
import ezvcard.parameter.ImageType;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.Address;
import ezvcard.property.Anniversary;
import ezvcard.property.Birthday;
import ezvcard.property.Categories;
import ezvcard.property.DateOrTimeProperty;
import ezvcard.property.Email;
import ezvcard.property.FormattedName;
import ezvcard.property.Impp;
import ezvcard.property.Kind;
import ezvcard.property.Logo;
import ezvcard.property.Member;
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
import ezvcard.util.PartialDate;
import lombok.Cleanup;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

@EqualsAndHashCode(exclude = { "categories", "unknownProperties" })
@ToString(exclude = { "photo" })
public class Contact {
    // productID (if set) will be used to generate a PRODID property.
    // You may set this statically from the calling application.
    public static String productID = null;

    public static final String
            PROPERTY_ADDRESSBOOKSERVER_KIND = "X-ADDRESSBOOKSERVER-KIND",
            PROPERTY_ADDRESSBOOKSERVER_MEMBER = "X-ADDRESSBOOKSERVER-MEMBER";

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

    public static final String DATE_PARAMETER_OMIT_YEAR = "X-APPLE-OMIT-YEAR";
    public static final int DATE_PARAMETER_OMIT_YEAR_DEFAULT = 1604;

    public String uid;
    public boolean group;

    /**
     * List of UIDs of group members without urn:uuid prefix (only meaningful if {@link #group} is true).
     */
    public List<String> members = new LinkedList<>();

    public String displayName;
    public String prefix, givenName, middleName, familyName, suffix;
    public String phoneticGivenName, phoneticMiddleName, phoneticFamilyName;
    public Nickname nickName;

    public Organization organization;
    public String   jobTitle,           // VCard TITLE
                    jobDescription;     // VCard ROLE

    public final List<LabeledProperty<Telephone>> phoneNumbers = new LinkedList<>();
    public final List<LabeledProperty<Email>> emails = new LinkedList<>();
    public final List<LabeledProperty<Impp>> impps = new LinkedList<>();
    public final List<LabeledProperty<Address>> addresses = new LinkedList<>();
    public final List<String> categories = new LinkedList<>();
    public final List<LabeledProperty<Url>> urls = new LinkedList<>();
    public final List<Related> relations = new LinkedList<>();

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


    @SuppressWarnings("LoopStatementThatDoesntLoop")
    protected static Contact fromVCard(VCard vCard, Downloader downloader) {
        Contact c = new Contact();

        // UID
        Uid uid = vCard.getUid();
        if (uid != null) {
            c.uid = uriToUID(uid.getValue());
            vCard.removeProperties(Uid.class);
        }
        if (c.uid == null) {
            Constants.log.warning("Received VCard without UID, generating new one");
            c.generateUID();
        }

        // KIND
        Kind kind = vCard.getKind();
        if (kind != null) {
            c.group = kind.isGroup();
            vCard.removeProperties(Kind.class);
        } else {
            // no KIND, try X-ADDRESSBOOKSERVER-KIND
            RawProperty xKind = vCard.getExtendedProperty(PROPERTY_ADDRESSBOOKSERVER_KIND);
            if (xKind != null && Kind.GROUP.equalsIgnoreCase(xKind.getValue()))
                c.group = true;
            vCard.removeExtendedProperty(PROPERTY_ADDRESSBOOKSERVER_KIND);
        }

        // MEMBER
        for (Member member : vCard.getMembers()) {
            String memberUID = uriToUID(member.getUri());
            if (memberUID != null)
                c.members.add(memberUID);
        }
        vCard.removeProperties(Member.class);
        for (RawProperty xMember : vCard.getExtendedProperties(PROPERTY_ADDRESSBOOKSERVER_MEMBER)) {
            String memberUID = uriToUID(xMember.getValue());
            if (memberUID != null)
                c.members.add(memberUID);
        }
        vCard.removeExtendedProperty(PROPERTY_ADDRESSBOOKSERVER_MEMBER);

        // FN
        FormattedName fn = vCard.getFormattedName();
        if (fn != null) {
            c.displayName = StringUtils.trimToNull(fn.getValue());
            vCard.removeProperties(FormattedName.class);
        }

        // N
        StructuredName n = vCard.getStructuredName();
        if (n != null) {
            c.prefix = StringUtils.trimToNull(StringUtils.join(n.getPrefixes(), ' '));
            c.givenName = StringUtils.trimToNull(n.getGiven());
            c.middleName = StringUtils.trimToNull(StringUtils.join(n.getAdditionalNames(), ' '));
            c.familyName = StringUtils.trimToNull(n.getFamily());
            c.suffix = StringUtils.trimToNull(StringUtils.join(n.getSuffixes(), ' '));
            vCard.removeProperties(StructuredName.class);
        }

        // phonetic names
        RawProperty phoneticFirstName = vCard.getExtendedProperty(PROPERTY_PHONETIC_FIRST_NAME),
                phoneticMiddleName = vCard.getExtendedProperty(PROPERTY_PHONETIC_MIDDLE_NAME),
                phoneticLastName = vCard.getExtendedProperty(PROPERTY_PHONETIC_LAST_NAME);
        if (phoneticFirstName != null) {
            c.phoneticGivenName = StringUtils.trimToNull(phoneticFirstName.getValue());
            vCard.removeExtendedProperty(PROPERTY_PHONETIC_FIRST_NAME);
        }
        if (phoneticMiddleName != null) {
            c.phoneticMiddleName = StringUtils.trimToNull(phoneticMiddleName.getValue());
            vCard.removeExtendedProperty(PROPERTY_PHONETIC_MIDDLE_NAME);
        }
        if (phoneticLastName != null) {
            c.phoneticFamilyName = StringUtils.trimToNull(phoneticLastName.getValue());
            vCard.removeExtendedProperty(PROPERTY_PHONETIC_LAST_NAME);
        }

        // X-ABLabel (in use with other properties as property group)
        List<RawProperty> labels = vCard.getExtendedProperties(LabeledProperty.PROPERTY_AB_LABEL);
        vCard.removeExtendedProperty(LabeledProperty.PROPERTY_AB_LABEL);

        // TEL
        for (Telephone phone : vCard.getTelephoneNumbers())
            c.phoneNumbers.add(new LabeledProperty<>(phone, findLabel(phone.getGroup(), labels)));
        vCard.removeProperties(Telephone.class);

        // EMAIL
        for (Email email : vCard.getEmails())
            c.emails.add(new LabeledProperty<>(email, findLabel(email.getGroup(), labels)));
        vCard.removeProperties(Email.class);

        // ORG
        c.organization = vCard.getOrganization();
        vCard.removeProperties(Organization.class);
        // TITLE
        for (Title title : vCard.getTitles()) {
            c.jobTitle = StringUtils.trimToNull(title.getValue());
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
        for (Impp impp :vCard.getImpps())
            c.impps.add(new LabeledProperty<>(impp, findLabel(impp.getGroup(), labels)));
        vCard.removeProperties(Impp.class);
        // add X-SIP properties as IMPP, too
        for (RawProperty sip : vCard.getExtendedProperties(PROPERTY_SIP))
            c.impps.add(new LabeledProperty<>(new Impp("sip", sip.getValue()), findLabel(sip.getGroup(), labels)));
        vCard.removeExtendedProperty(PROPERTY_SIP);

        // NICKNAME
        c.nickName = vCard.getNickname();
        vCard.removeProperties(Nickname.class);

        // ADR
        for (Address address : vCard.getAddresses())
            c.addresses.add(new LabeledProperty<>(address, findLabel(address.getGroup(), labels)));
        vCard.removeProperties(Address.class);

        // NOTE
        List<String> notes = new LinkedList<>();
        for (Note note : vCard.getNotes())
            notes.add(note.getValue());
        if (!notes.isEmpty())
            c.note = StringUtils.trimToNull(StringUtils.join(notes, "\n\n\n"));
        vCard.removeProperties(Note.class);

        // CATEGORY
        Categories categories = vCard.getCategories();
        if (categories != null)
            c.categories.addAll(categories.getValues());
        vCard.removeProperties(Categories.class);

        // URL
        for (Url url : vCard.getUrls())
            c.urls.add(new LabeledProperty<>(url, findLabel(url.getGroup(), labels)));
        vCard.removeProperties(Url.class);

        // BDAY
        c.birthDay = vCard.getBirthday();
        checkVCard3PartialDate(c.birthDay);
        vCard.removeProperties(Birthday.class);
        // ANNIVERSARY
        c.anniversary = vCard.getAnniversary();
        checkVCard3PartialDate(c.anniversary);
        vCard.removeProperties(Anniversary.class);

        // RELATED
        for (Related related : vCard.getRelations()) {
            String text = related.getText();
            if (!StringUtils.isEmpty(text)) {
                // process only free-form relations with text
                c.relations.add(related);
            }
        }
        vCard.removeProperties(Related.class);

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
                Constants.log.warning("Couldn't serialize unknown properties, dropping them");
            }

        return c;
    }

    public void write(VCardVersion vCardVersion, GroupMethod groupMethod, OutputStream os) throws IOException {
        VCard vCard = null;
        try {
            if (unknownProperties != null)
                vCard = Ezvcard.parse(unknownProperties).first();
        } catch (Exception e) {
            Constants.log.fine("Couldn't parse original VCard, creating from scratch");
        }
        if (vCard == null)
            vCard = new VCard();

        // UID
        if (uid != null)
            vCard.setUid(new Uid(uid));
        else
            Constants.log.severe("Generating VCard without UID");

        // PRODID
        if (productID != null)
            vCard.setProductId(productID);

        // group support
        if (group && groupMethod == GroupMethod.GROUP_VCARDS) {
            if (vCardVersion == VCardVersion.V4_0) {
                vCard.setKind(Kind.group());
                for (String uid : members)
                    vCard.addMember(new Member("urn:uuid:" + uid));
            } else { // "VCard4 as VCard3" (Apple-style)
                vCard.setExtendedProperty(PROPERTY_ADDRESSBOOKSERVER_KIND, Kind.GROUP);
                for (String uid : members)
                    vCard.addExtendedProperty(PROPERTY_ADDRESSBOOKSERVER_MEMBER, "urn:uuid:" + uid);
            }
        }
        // CATEGORIES
        if (!categories.isEmpty())
            vCard.setCategories(categories.toArray(new String[categories.size()]));

        // FN
        String fn = displayName;
        if (StringUtils.isEmpty(fn) && organization != null)
            for (String part : organization.getValues()) {
                fn = part;
                if (!StringUtils.isEmpty(fn))
                    break;
            }
        if (StringUtils.isEmpty(fn) && nickName != null)
            fn = nickName.getValues().get(0);
        if (StringUtils.isEmpty(fn) && !emails.isEmpty())
            fn = emails.get(0).property.getValue();
        if (StringUtils.isEmpty(fn) && !phoneNumbers.isEmpty())
            fn = phoneNumbers.get(0).property.getText();
        if (StringUtils.isEmpty(fn))
            fn = uid;
        if (StringUtils.isEmpty(fn))
            fn = "";
        vCard.setFormattedName(fn);

        // N
        if (prefix != null || familyName != null || middleName != null || givenName != null || suffix != null) {
            StructuredName n = new StructuredName();
            if (prefix != null)
                for (String p : StringUtils.split(prefix, ' '))
                    n.getPrefixes().add(p);
            n.setGiven(givenName);
            if (middleName != null)
                for (String middle : StringUtils.split(middleName, ' '))
                    n.getAdditionalNames().add(middle);
            n.setFamily(familyName);
            if (suffix != null)
                for (String s : StringUtils.split(suffix, ' '))
                    n.getSuffixes().add(s);
            vCard.setStructuredName(n);

        } else if (vCardVersion == VCardVersion.V3_0) {
            // (only) VCard 3 requires N [RFC 2426 3.1.2]
            if (group && groupMethod == GroupMethod.GROUP_VCARDS) {
                StructuredName n = new StructuredName();
                n.setFamily(fn);
                vCard.setStructuredName(n);
            } else
                vCard.setStructuredName(new StructuredName());
        }

        // phonetic names
        if (phoneticGivenName != null)
            vCard.addExtendedProperty(PROPERTY_PHONETIC_FIRST_NAME, phoneticGivenName);
        if (phoneticMiddleName != null)
            vCard.addExtendedProperty(PROPERTY_PHONETIC_MIDDLE_NAME, phoneticMiddleName);
        if (phoneticFamilyName != null)
            vCard.addExtendedProperty(PROPERTY_PHONETIC_LAST_NAME, phoneticFamilyName);

        // will be used to count "davdroidXX." property groups
        AtomicInteger labelIterator = new AtomicInteger();

        // TEL
        for (LabeledProperty<Telephone> labeledPhone : phoneNumbers) {
            Telephone phone = labeledPhone.property;
            vCard.addTelephoneNumber(phone);
            addLabel(labeledPhone, labelIterator, vCard);
        }

        // EMAIL
        for (LabeledProperty<Email> labeledEmail : emails) {
            Email email = labeledEmail.property;
            vCard.addEmail(email);
            addLabel(labeledEmail, labelIterator, vCard);
        }

        // ORG, TITLE, ROLE
        if (organization != null)
            vCard.setOrganization(organization);
        if (jobTitle != null)
            vCard.addTitle(jobTitle);
        if (jobDescription != null)
            vCard.addRole(jobDescription);

        // IMPP
        for (LabeledProperty<Impp> labeledImpp : impps) {
            Impp impp = labeledImpp.property;
            vCard.addImpp(impp);
            addLabel(labeledImpp, labelIterator, vCard);
        }

        // NICKNAME
        if (nickName != null)
            vCard.setNickname(nickName);

        // ADR
        for (LabeledProperty<Address> labeledAddress : addresses) {
            Address address = labeledAddress.property;
            vCard.addAddress(address);
            addLabel(labeledAddress, labelIterator, vCard);
        }

        // NOTE
        if (note != null)
            vCard.addNote(note);

        // URL
        for (LabeledProperty<Url> labeledUrl : urls) {
            Url url = labeledUrl.property;
            vCard.addUrl(url);
            addLabel(labeledUrl, labelIterator, vCard);
        }

        // ANNIVERSARY
        if (anniversary != null) {
            if (vCardVersion == VCardVersion.V4_0 || anniversary.getDate() != null)
                vCard.setAnniversary(anniversary);
            else if (anniversary.getPartialDate() != null) {
                // VCard 3: partial date with month and day, but without year
                PartialDate partial = anniversary.getPartialDate();
                if (partial.getDate() != null && partial.getMonth() != null) {
                    if (partial.getYear() != null)
                        // partial date is a complete date
                        vCard.setAnniversary(anniversary);
                    else {
                        // VCard 3: partial date with month and day, but without year
                        Calendar fakeCal = GregorianCalendar.getInstance();
                        fakeCal.set(DATE_PARAMETER_OMIT_YEAR_DEFAULT, partial.getMonth()-1, partial.getDate());
                        Anniversary fakeAnniversary = new Anniversary(fakeCal.getTime(), false);
                        fakeAnniversary.addParameter(DATE_PARAMETER_OMIT_YEAR, String.valueOf(DATE_PARAMETER_OMIT_YEAR_DEFAULT));
                        vCard.setAnniversary(fakeAnniversary);
                    }
                }
            }
        }
        // BDAY
        if (birthDay != null) {
            if (vCardVersion == VCardVersion.V4_0 || birthDay.getDate() != null)
                vCard.setBirthday(birthDay);
            else if (birthDay.getPartialDate() != null) {
                PartialDate partial = birthDay.getPartialDate();
                if (partial.getDate() != null && partial.getMonth() != null) {
                    if (partial.getYear() != null)
                        // partial date is a complete date
                        vCard.setBirthday(birthDay);
                    else {
                        // VCard 3: partial date with month and day, but without year
                        Calendar fakeCal = GregorianCalendar.getInstance();
                        fakeCal.set(DATE_PARAMETER_OMIT_YEAR_DEFAULT, partial.getMonth()-1, partial.getDate());
                        Birthday fakeBirthday = new Birthday(fakeCal.getTime(), false);
                        fakeBirthday.addParameter(DATE_PARAMETER_OMIT_YEAR, String.valueOf(DATE_PARAMETER_OMIT_YEAR_DEFAULT));
                        vCard.setBirthday(fakeBirthday);
                    }
                }
            }
        }

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
            Constants.log.warning("Generating possibly invalid VCard:");
            for (Map.Entry<VCardProperty, List<ValidationWarning>> entry : validation)
                for (ValidationWarning warning : entry.getValue())
                    if (entry.getKey() != null)
                        Constants.log.warning("  * " + entry.getKey().getClass().getSimpleName() + " - " + warning.getMessage());
        }

        // generate VCARD
        Ezvcard .write(vCard)
                .version(vCardVersion)
                .versionStrict(false)      // allow VCard4 properties in VCard3s
                .caretEncoding(true)       // enable RFC 6868 support
                .prodId(productID == null)
                .go(os);
    }


    protected void generateUID() {
        uid = UUID.randomUUID().toString();
    }

    public static String uriToUID(String uriString) {
        URI uri = null;
        try {
            uri = new URI(uriString);

            if (uri.getScheme() == null)
                return uri.getSchemeSpecificPart();
            else if ("urn".equalsIgnoreCase(uri.getScheme()) && StringUtils.startsWithIgnoreCase(uri.getSchemeSpecificPart(), "uuid:"))
                return uri.getSchemeSpecificPart().substring(5);
            else
                return null;
        } catch(URISyntaxException e) {
            Constants.log.warning("Invalid URI for UID: " + uri);
            return uriString;
        }
    }

    private static void addLabel(LabeledProperty<? extends VCardProperty> labeledUrl, AtomicInteger labelIterator, VCard vCard) {
        if (labeledUrl.label != null) {
            String group = "davdroid" + labelIterator.incrementAndGet();
            labeledUrl.property.setGroup(group);

            RawProperty label = vCard.addExtendedProperty(LabeledProperty.PROPERTY_AB_LABEL, labeledUrl.label);
            label.setGroup(group);
        }
    }

    private static String findLabel(String group, List<RawProperty> labels) {
        if (labels == null || group == null)
            return null;
        for (RawProperty label : labels)
            if (StringUtils.equalsIgnoreCase(label.getGroup(), group))
                return label.getValue();
        return null;
    }

    protected static void checkVCard3PartialDate(DateOrTimeProperty property) {
        if (property != null && property.getDate() != null) {
            String omitYearStr = property.getParameter(DATE_PARAMETER_OMIT_YEAR);
            if (omitYearStr != null)
                try {
                    int omitYear = Integer.parseInt(omitYearStr);
                    Calendar cal = GregorianCalendar.getInstance();
                    cal.setTime(property.getDate());
                    if (cal.get(GregorianCalendar.YEAR) == omitYear) {
                        PartialDate partial = PartialDate.builder()
                                .date(cal.get(GregorianCalendar.DAY_OF_MONTH))
                                .month(cal.get(GregorianCalendar.MONTH)+1)
                                .build();
                        property.setPartialDate(partial);
                    }
                } catch(NumberFormatException e) {
                    Constants.log.log(Level.WARNING, "Unparseable " + DATE_PARAMETER_OMIT_YEAR);
                } finally {
                    property.removeParameter(DATE_PARAMETER_OMIT_YEAR);
                }
        }
    }


    public interface Downloader {
        byte[] download(String url, String accepts);
    }

}

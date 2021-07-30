package at.bitfire.vcard4android

import at.bitfire.vcard4android.property.*
import ezvcard.VCard
import ezvcard.VCardDataType
import ezvcard.VCardVersion
import ezvcard.parameter.ImageType
import ezvcard.property.*
import ezvcard.util.PartialDate
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.net.URI
import java.time.ZoneOffset
import java.util.*

class ContactWriterTest {

    // test specific fields

    @Test
    fun testAddress() {
        val address = Address().apply {
            streetAddress = "Test Street"
            country = "XX"
        }
        val vCard = generate {
            addresses.add(LabeledProperty(address))
        }
        assertEquals(address, vCard.addresses.first())
    }


    @Test
    fun testAnniversary_vCard3() {
        val date = Date(121, 6, 30)
        val vCard = generate(version = VCardVersion.V3_0) {
            anniversary = Anniversary(date)
        }
        assertNull(vCard.anniversary)
        assertEquals(date, vCard.getProperty(XAbDate::class.java).date)
    }

    @Test
    fun testAnniversary_vCard4() {
        val ann = Anniversary(Date(121, 6, 30))
        val vCard = generate(version = VCardVersion.V4_0) {
            anniversary = ann
        }
        assertEquals(ann, vCard.anniversary)
    }


    @Test
    fun testBirthday() {
        val bday = Birthday(Date(121, 6, 30))
        val vCard = generate {
            birthDay = bday
        }
        assertEquals(bday, vCard.birthday)
    }


    @Test
    fun testCustomDate() {
        val date = XAbDate(Date(121, 6, 30))
        val vCard = generate {
            customDates += LabeledProperty(date)
        }
        assertEquals(date, vCard.getProperty(XAbDate::class.java))
    }


    @Test
    fun testCategories_Some() {
        val vCard = generate {
            categories += "cat1"
            categories += "cat2"
        }
        assertEquals("cat1", vCard.categories.values[0])
        assertEquals("cat2", vCard.categories.values[1])
    }

    @Test
    fun testCategories_None() {
        val vCard = generate { }
        assertNull(vCard.categories)
    }


    @Test
    fun testEmail() {
        val vCard = generate {
            emails.add(LabeledProperty(Email("test@example.com")))
        }
        assertEquals("test@example.com", vCard.emails.first().value)
    }


    @Test
    fun testFn_vCard3_NoFn_Organization() {
        val vCard = generate(version = VCardVersion.V3_0) {
            organization = Organization().apply {
                values.add("org")
                values.add("dept")
            }
            // other values should be ignored because organization is available
            nickName = LabeledProperty(Nickname().apply {
                values.add("nick1")
            })
        }
        assertEquals("org / dept", vCard.formattedName.value)
    }

    @Test
    fun testFn_vCard3_NoFn_NickName() {
        val vCard = generate(version = VCardVersion.V3_0) {
            nickName = LabeledProperty(Nickname().apply {
                values.add("nick1")
            })
            // other values should be ignored because nickname is available
            emails += LabeledProperty(Email("test@example.com"))
        }
        assertEquals("nick1", vCard.formattedName.value)
    }

    @Test
    fun testFn_vCard3_NoFn_Email() {
        val vCard = generate(version = VCardVersion.V3_0) {
            emails += LabeledProperty(Email("test@example.com"))
            // other values should be ignored because email is available
            phoneNumbers += LabeledProperty(Telephone("+1 555 12345"))
        }
        assertEquals("test@example.com", vCard.formattedName.value)
    }

    @Test
    fun testFn_vCard3_NoFn_Phone() {
        val vCard = generate(version = VCardVersion.V3_0) {
            phoneNumbers += LabeledProperty(Telephone("+1 555 12345"))
            // other values should be ignored because phone is available
            uid = "uid"
        }
        assertEquals("+1 555 12345", vCard.formattedName.value)
    }

    @Test
    fun testFn_vCard3_NoFn_Uid() {
        val vCard = generate(version = VCardVersion.V3_0) {
            uid = "uid"
        }
        assertEquals("uid", vCard.formattedName.value)
    }

    @Test
    fun testFn_vCard3_NoFn_Nothing() {
        val vCard = generate(version = VCardVersion.V3_0) { }
        assertEquals("", vCard.formattedName.value)
    }

    @Test
    fun testFn_vCard3_Fn() {
        val vCard = generate(version = VCardVersion.V3_0) {
            displayName = "Display Name"
        }
        assertEquals("Display Name", vCard.formattedName.value)
    }

    @Test
    fun testFn_vCard4_NoFn() {
        val vCard = generate(version = VCardVersion.V4_0) { }
        assertNull(vCard.formattedName)
    }

    @Test
    fun testFn_vCard4_Fn() {
        val vCard = generate(version = VCardVersion.V4_0) {
            displayName = "Display Name"
        }
        assertEquals("Display Name", vCard.formattedName.value)
    }


    @Test
    fun testImpp() {
        val vCard = generate {
            impps.add(LabeledProperty(Impp.xmpp("test@example.com")))
        }
        assertEquals(URI("xmpp:test@example.com"), vCard.impps.first().uri)
    }


    @Test
    fun testKindAndMember_vCard3() {
        val vCard = generate(GroupMethod.GROUP_VCARDS, VCardVersion.V3_0) {
            group = true
            members += "member1"
        }
        assertEquals(Kind.GROUP, vCard.getProperty(XAddressBookServerKind::class.java).value)
        assertEquals("urn:uuid:member1", vCard.getProperty(XAddressBookServerMember::class.java).value)
    }

    @Test
    fun testKindAndMember_vCard4() {
        val vCard = generate(GroupMethod.GROUP_VCARDS, VCardVersion.V4_0) {
            group = true
            members += "member1"
        }
        assertEquals(Kind.GROUP, vCard.getProperty(Kind::class.java).value)
        assertEquals("urn:uuid:member1", vCard.members.first().value)
    }


    @Test
    fun testN_vCard3_NoN() {
        val vCard = generate(version = VCardVersion.V3_0) { }
        assertEquals(StructuredName(), vCard.structuredName)
    }

    @Test
    fun testN_vCard4_NoN() {
        val vCard = generate(version = VCardVersion.V4_0) { }
        assertNull(vCard.structuredName)
    }

    @Test
    fun testN() {
        val vCard = generate(version = VCardVersion.V4_0) {
            prefix = "P1. P2."
            givenName = "Given"
            middleName = "Middle1 Middle2"
            familyName = "Family"
            suffix = "S1 S2"
        }
        assertEquals(StructuredName().apply {
            prefixes += "P1."
            prefixes += "P2."
            given = "Given"
            additionalNames += "Middle1"
            additionalNames += "Middle2"
            family = "Family"
            suffixes += "S1"
            suffixes += "S2"
        }, vCard.structuredName)
    }


    @Test
    fun testNote() {
        val vCard = generate { note = "Some Note" }
        assertEquals("Some Note", vCard.notes.first().value)
    }


    @Test
    fun testOrganization() {
        val org = Organization().apply {
            values.add("Org")
            values.add("Dept")
        }
        val vCard = generate {
            organization = org
            jobTitle = "CEO"
            jobDescription = "Executive"
        }
        assertEquals(org, vCard.organization)
        assertEquals("CEO", vCard.titles.first().value)
        assertEquals("Executive", vCard.roles.first().value)
    }


    @Test
    fun testPhoto() {
        val testPhoto = ByteArray(128)
        val vCard = generate { photo = testPhoto }
        assertEquals(Photo(testPhoto, ImageType.JPEG), vCard.photos.first())
    }


    @Test
    fun testRelation() {
        val rel = Related.email("bigbrother@example.com")
        val vCard = generate { relations += rel }
        assertEquals(rel, vCard.relations.first())
    }


    @Test
    fun testTel() {
        val vCard = generate {
            phoneNumbers.add(LabeledProperty(Telephone("+1 555 12345")))
        }
        assertEquals("+1 555 12345", vCard.telephoneNumbers.first().text)
    }


    @Test
    fun testUid() {
        val vCard = generate { uid = "12345" }
        assertEquals("12345", vCard.uid.value)
    }


    @Test
    fun testUnknownProperty() {
        val vCard = generate {
            unknownProperties = "BEGIN:VCARD\r\n" +
                    "FUTURE-PROPERTY;X-TEST=test1;TYPE=uri:12345\r\n" +
                    "END:VCARD\r\n"
        }
        assertEquals("12345", vCard.getExtendedProperty("FUTURE-PROPERTY").value)
        assertEquals("test1", vCard.getExtendedProperty("FUTURE-PROPERTY").getParameter("X-TEST"))
    }


    @Test
    fun testUrl() {
        val vCard = generate { urls += LabeledProperty(Url("https://example.com")) }
        assertEquals("https://example.com", vCard.urls.first().value)
    }


    @Test
    fun testXPhoneticName() {
        val vCard = generate() {
            phoneticGivenName = "Given"
            phoneticMiddleName = "Middle"
            phoneticFamilyName = "Family"
        }
        assertEquals("Given", vCard.getProperty(XPhoneticFirstName::class.java).value)
        assertEquals("Middle", vCard.getProperty(XPhoneticMiddleName::class.java).value)
        assertEquals("Family", vCard.getProperty(XPhoneticLastName::class.java).value)
    }



    // test generator helpers

    @Test
    fun testAddLabeledProperty_NoLabel() {
        val vCard = generate {
            nickName = LabeledProperty(Nickname().apply {
                values.add("nick1")
            })
        }
        assertEquals(2 /* NICK + REV */, vCard.properties.size)
        assertEquals("nick1", vCard.nickname.values.first())
    }

    @Test
    fun testAddLabeledProperty_Label() {
        val vCard = generate {
            nickName = LabeledProperty(Nickname().apply {
                values.add("nick1")
            }, "label1")
        }
        assertEquals(3 /* NICK + X-ABLABEL + REV */, vCard.properties.size)
        vCard.nickname.apply {
            assertEquals("nick1", values.first())
            assertEquals("item1", group)
        }
        vCard.getProperty(XAbLabel::class.java).apply {
            assertEquals("label1", value)
            assertEquals("item1", group)
        }
    }

    @Test
    fun testAddLabeledProperty_Label_CollisionWithUnknownProperty() {
        val vCard = generate {
            unknownProperties = "BEGIN:VCARD\n" +
                    "item1.X-TEST:This property is blocking the first item ID\n" +
                    "END:VCARD"
            nickName = LabeledProperty(Nickname().apply {
                values.add("nick1")
            }, "label1")
        }
        assertEquals(4 /* X-TEST + NICK + X-ABLABEL + REV */, vCard.properties.size)
        vCard.nickname.apply {
            assertEquals("nick1", values.first())
            assertEquals("item2", group)
        }
        vCard.getProperty(XAbLabel::class.java).apply {
            assertEquals("label1", value)
            assertEquals("item2", group)
        }
    }


    @Test
    fun testRewritePartialDate_vCard3_Date() {
        val generator = ContactWriter.fromContact(Contact(), VCardVersion.V3_0, GroupMethod.GROUP_VCARDS)
        val date = Birthday(Date(121, 6, 30))
        generator.rewritePartialDate(date)
        assertEquals(Date(121, 6, 30), date.date)
        assertNull(date.partialDate)
    }

    @Test
    fun testRewritePartialDate_vCard4_Date() {
        val generator = ContactWriter.fromContact(Contact(), VCardVersion.V4_0, GroupMethod.GROUP_VCARDS)
        val date = Birthday(Date(121, 6, 30))
        generator.rewritePartialDate(date)
        assertEquals(Date(121, 6, 30), date.date)
        assertNull(date.partialDate)
        assertEquals(0, date.parameters.size())
    }

    @Test
    fun testRewritePartialDate_vCard3_PartialDateWithYear() {
        val generator = ContactWriter.fromContact(Contact(), VCardVersion.V3_0, GroupMethod.GROUP_VCARDS)
        val date = Birthday(PartialDate.parse("20210730"))
        generator.rewritePartialDate(date)
        assertEquals(Date(121, 6, 30), date.date)
        assertNull(date.partialDate)
        assertEquals(0, date.parameters.size())
    }

    @Test
    fun testRewritePartialDate_vCard4_PartialDateWithYear() {
        val generator = ContactWriter.fromContact(Contact(), VCardVersion.V4_0, GroupMethod.GROUP_VCARDS)
        val date = Birthday(PartialDate.parse("20210730"))
        generator.rewritePartialDate(date)
        assertNull(date.date)
        assertEquals(PartialDate.parse("20210730"), date.partialDate)
        assertEquals(0, date.parameters.size())
    }

    @Test
    fun testRewritePartialDate_vCard3_PartialDateWithoutYear() {
        val generator = ContactWriter.fromContact(Contact(), VCardVersion.V3_0, GroupMethod.GROUP_VCARDS)
        val date = Birthday(PartialDate.parse("--0730"))
        generator.rewritePartialDate(date)
        assertEquals(Date(-300+4, 6, 30), date.date)
        assertNull(date.partialDate)
        assertEquals(1, date.parameters.size())
        assertEquals("1604", date.getParameter(Contact.DATE_PARAMETER_OMIT_YEAR))
    }

    @Test
    fun testRewritePartialDate_vCard4_PartialDateWithoutYear() {
        val generator = ContactWriter.fromContact(Contact(), VCardVersion.V4_0, GroupMethod.GROUP_VCARDS)
        val date = Birthday(PartialDate.parse("--0730"))
        generator.rewritePartialDate(date)
        assertNull(date.date)
        assertEquals(PartialDate.parse("--0730"), date.partialDate)
        assertEquals(0, date.parameters.size())
    }


    @Test
    fun testWriteVCard() {
        val generator = ContactWriter.fromContact(Contact(), VCardVersion.V4_0, GroupMethod.GROUP_VCARDS)
        generator.vCard.revision = Revision(Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC.id)).apply {
            set(2021, 6, 30, 1, 2, 3)
        })

        val stream = ByteArrayOutputStream()
        generator.writeVCard(stream)
        assertEquals("BEGIN:VCARD\r\n" +
                "VERSION:4.0\r\n" +
                "PRODID:ez-vcard 0.11.2\r\n" +
                "REV:20210730T010203Z\r\n" +
                "END:VCARD\r\n", stream.toString())
    }

    @Test
    fun testWriteVCard_CaretEncoding() {
        val stream = ByteArrayOutputStream()
        val contact = Contact().apply {
            addresses += LabeledProperty(Address().apply {
                label = "Li^ne 1,1 - \" -"
                streetAddress = "Line1"
                country = "Line2"
            })
        }
        ContactWriter
                .fromContact(contact, VCardVersion.V4_0, GroupMethod.GROUP_VCARDS)
                .writeVCard(stream)
        assertTrue(stream.toString().contains("ADR;LABEL=\"Li^^ne 1,1 - ^' -\":;;Line1;;;;Line2"))
    }


    // helpers

    private fun generate(groupMethod: GroupMethod = GroupMethod.GROUP_VCARDS, version: VCardVersion = VCardVersion.V4_0, prepare: Contact.() -> Unit): VCard {
        val contact = Contact()
        contact.run(prepare)
        return ContactWriter.fromContact(contact, version, groupMethod).vCard
    }

}
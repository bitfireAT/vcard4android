/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds
import at.bitfire.vcard4android.Contact
import ezvcard.property.Organization
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OrganizationBuilderTest {

    @Test
    fun testEmpty() {
        OrganizationBuilder(Uri.EMPTY, null, Contact(), false).build().also { result ->
            assertEquals(0, result.size)
        }
    }

    @Test
    fun testEmpty_OrganizationEmpty() {
        OrganizationBuilder(Uri.EMPTY, null, Contact().apply {
            organization = Organization()
        }, false).build().also { result ->
            assertEquals(0, result.size)
        }
    }


    @Test
    fun testJobDescription() {
        OrganizationBuilder(Uri.EMPTY, null, Contact().apply {
            jobDescription = "Job Description"
        }, false).build().also { result ->
            assertEquals("Job Description", result[0].values[CommonDataKinds.Organization.JOB_DESCRIPTION])
        }
    }


    @Test
    fun testMimeType() {
        OrganizationBuilder(Uri.EMPTY, null, Contact().apply {
            jobDescription = "Job Description"
        }, false).build().also { result ->
            assertEquals(CommonDataKinds.Organization.CONTENT_ITEM_TYPE, result[0].values[CommonDataKinds.Organization.MIMETYPE])
        }
    }


    @Test
    fun testOrganization_OnlyCompany() {
        OrganizationBuilder(Uri.EMPTY, null, Contact().apply {
            organization = Organization().apply {
                values.add("Organization")
            }
        }, false).build().also { result ->
            assertEquals("Organization", result[0].values[CommonDataKinds.Organization.COMPANY])
            assertNull(result[0].values[CommonDataKinds.Organization.DEPARTMENT])
        }
    }

    @Test
    fun testOrganization_Company_Department() {
        OrganizationBuilder(Uri.EMPTY, null, Contact().apply {
            organization = Organization().apply {
                values.add("Organization")
                values.add("Department")
            }
        }, false).build().also { result ->
            assertEquals("Organization", result[0].values[CommonDataKinds.Organization.COMPANY])
            assertEquals("Department", result[0].values[CommonDataKinds.Organization.DEPARTMENT])
        }
    }

    @Test
    fun testOrganization_Company_Departments() {
        OrganizationBuilder(Uri.EMPTY, null, Contact().apply {
            organization = Organization().apply {
                values.add("Organization")
                values.add("Department")
                values.add("Division")
            }
        }, false).build().also { result ->
            assertEquals("Organization", result[0].values[CommonDataKinds.Organization.COMPANY])
            assertEquals("Department / Division", result[0].values[CommonDataKinds.Organization.DEPARTMENT])
        }
    }


    @Test
    fun testTitle() {
        OrganizationBuilder(Uri.EMPTY, null, Contact().apply {
            jobTitle = "Job Title"
        }, false).build().also { result ->
            assertEquals("Job Title", result[0].values[CommonDataKinds.Organization.TITLE])
        }
    }

}
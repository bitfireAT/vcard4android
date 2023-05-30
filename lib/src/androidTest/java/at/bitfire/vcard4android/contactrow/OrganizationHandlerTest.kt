/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.content.ContentValues
import android.provider.ContactsContract.CommonDataKinds.Organization
import at.bitfire.vcard4android.Contact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OrganizationHandlerTest {

    @Test
    fun testJobDescription() {
        val contact = Contact()
        OrganizationHandler.handle(ContentValues().apply {
            put(Organization.JOB_DESCRIPTION, "Job Description")
        }, contact)
        assertEquals("Job Description", contact.jobDescription)
    }


    @Test
    fun testJobTitle() {
        val contact = Contact()
        OrganizationHandler.handle(ContentValues().apply {
            put(Organization.TITLE, "Job Title")
        }, contact)
        assertEquals("Job Title", contact.jobTitle)
    }


    @Test
    fun testOrganization_Empty() {
        val contact = Contact()
        OrganizationHandler.handle(ContentValues().apply {
            putNull(Organization.COMPANY)
            putNull(Organization.DEPARTMENT)
        }, contact)
        assertNull(contact.organization)
    }

    @Test
    fun testOrganization_Company() {
        val contact = Contact()
        OrganizationHandler.handle(ContentValues().apply {
            put(Organization.COMPANY, "Company")
        }, contact)
        assertEquals(1, contact.organization!!.values.size)
        assertEquals("Company", contact.organization!!.values.first())
    }

    @Test
    fun testOrganization_CompanyDepartment() {
        val contact = Contact()
        OrganizationHandler.handle(ContentValues().apply {
            put(Organization.COMPANY, "Company")
            put(Organization.DEPARTMENT, "Department")
        }, contact)
        assertEquals(2, contact.organization!!.values.size)
        assertEquals("Company", contact.organization!!.values[0])
        assertEquals("Department", contact.organization!!.values[1])
    }

    @Test
    fun testOrganization_Department() {
        val contact = Contact()
        OrganizationHandler.handle(ContentValues().apply {
            put(Organization.DEPARTMENT, "Department")
        }, contact)
        assertEquals(1, contact.organization!!.values.size)
        assertEquals("Department", contact.organization!!.values[0])
    }

}
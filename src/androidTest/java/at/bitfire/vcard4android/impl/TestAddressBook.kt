/*
 * Copyright © Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.vcard4android.impl

import android.accounts.Account
import android.content.ContentProviderClient
import at.bitfire.vcard4android.*

class TestAddressBook(
        account: Account,
        provider: ContentProviderClient
): AndroidAddressBook<AndroidContact, AndroidGroup>(account, provider, ContactFactory, GroupFactory) {

    object ContactFactory: AndroidContactFactory<AndroidContact> {

        override fun newInstance(addressBook: AndroidAddressBook<AndroidContact, AndroidGroup>, id: Long, fileName: String?, eTag: String?) =
                AndroidContact(addressBook, id, fileName, eTag)

        override fun newInstance(addressBook: AndroidAddressBook<AndroidContact, AndroidGroup>, contact: Contact, fileName: String?, eTag: String?): AndroidContact =
                AndroidContact(addressBook, contact, fileName, eTag)

    }


    object GroupFactory: AndroidGroupFactory<AndroidGroup> {

        override fun newInstance(addressBook: AndroidAddressBook<AndroidContact, AndroidGroup>, id: Long, fileName: String?, eTag: String?) =
                AndroidGroup(addressBook, id, fileName, eTag)

        override fun newInstance(addressBook: AndroidAddressBook<AndroidContact, AndroidGroup>, contact: Contact, fileName: String?, eTag: String?) =
                AndroidGroup(addressBook, contact, fileName, eTag)

    }
    
}
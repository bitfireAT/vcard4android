/*
 * Copyright © Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.vcard4android

interface AndroidGroupFactory<T: AndroidGroup> {

    fun newInstance(addressBook: AndroidAddressBook<out AndroidContact, T>, id: Long, fileName: String?, eTag: String?): T
    fun newInstance(addressBook: AndroidAddressBook<out AndroidContact, T>, contact: Contact, fileName: String?, eTag: String?): T

}
/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android

import android.content.ContentValues

interface AndroidGroupFactory<T: AndroidGroup> {

    fun fromProvider(addressBook: AndroidAddressBook<out AndroidContact, T>, values: ContentValues): T

}

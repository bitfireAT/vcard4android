/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android

class ContactsStorageException @JvmOverloads constructor(
        message: String?,
        ex: Throwable? = null
): Exception(message, ex)

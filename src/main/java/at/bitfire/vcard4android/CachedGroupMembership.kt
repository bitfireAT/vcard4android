/*
 * Copyright © 2013 – 2016 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.vcard4android

import android.provider.ContactsContract.RawContacts.Data

object CachedGroupMembership {

    @JvmField val CONTENT_ITEM_TYPE = "x.davdroid/cached-group-membership"

    @JvmField val MIMETYPE = Data.MIMETYPE
    @JvmField val RAW_CONTACT_ID = Data.RAW_CONTACT_ID
    @JvmField val GROUP_ID = Data.DATA1

}

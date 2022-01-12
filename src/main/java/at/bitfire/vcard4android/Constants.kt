/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android

import java.util.logging.Level
import java.util.logging.Logger

object Constants {

    val log: Logger = Logger.getLogger("vcard4android")

    init {
        if (BuildConfig.DEBUG)
            log.level = Level.ALL
    }

}
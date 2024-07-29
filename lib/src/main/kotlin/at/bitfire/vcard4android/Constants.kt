/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android

import java.util.logging.Logger

object Constants {

    @Deprecated("Use java.util.Logger.getLogger(javaClass.name) instead", ReplaceWith("Logger.getLogger(javaClass.name)", "java.util.logging.Logger"))
    val log: Logger = Logger.getLogger("at.bitfire.vcard4android")

}
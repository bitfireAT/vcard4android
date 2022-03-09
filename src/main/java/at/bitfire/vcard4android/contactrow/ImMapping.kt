/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import at.bitfire.vcard4android.Constants
import at.bitfire.vcard4android.Utils.normalizeNFD
import java.net.URI
import java.net.URISyntaxException
import java.util.logging.Level

object ImMapping {

    const val MESSENGER_AIM = "AIM"
    const val MESSENGER_IRC = "IRC"
    const val MESSENGER_THREEMA = "Threema"

    // TODO Tests


    fun messengerToUri(messenger: String, handle: String): URI? =
        try {
            when (messenger.lowercase()) {
                MESSENGER_AIM.lowercase() -> URI("aim", handle, null)
                MESSENGER_IRC.lowercase() -> URI("irc", handle, null)
                MESSENGER_THREEMA.lowercase() -> URI("https", "threema.id", "/${handle}", null)
                else ->
                    // fallback for unknown messengers
                    URI(messengerToUriScheme(messenger), handle, null)
            }
        } catch (e: URISyntaxException) {
            Constants.log.log(Level.WARNING, "Couldn't generate URI from IM: $messenger / $handle", e)
            null
        }

    fun uriToMessenger(uri: URI): Pair<String, String>? =
        when {
            uri.scheme.equals("aim", true) ->
                Pair(MESSENGER_AIM, uri.schemeSpecificPart)
            uri.scheme.equals("irc", true) ->
                Pair(MESSENGER_IRC, uri.schemeSpecificPart)
            uri.authority.equals("threema.id", true) ->
                Pair(MESSENGER_THREEMA, uri.path.trimStart('/'))
            else -> null
        }


    fun messengerToUriScheme(s: String?) = s
        ?.normalizeNFD()     // normalize with decomposition first (e.g. Á → A+ ́)

        /* then filter according to RFC 3986 3.1:
           scheme      = ALPHA *( ALPHA / DIGIT / "+" / "-" / "." )
           ALPHA       =  %x41-5A / %x61-7A   ; A-Z / a-z
           DIGIT       =  %x30-39             ; 0-9
        */
        ?.replace(Regex("^[^a-zA-Z]+"), "")
        ?.replace(Regex("[^\\da-zA-Z+-.]"), "")
        ?.lowercase()

}
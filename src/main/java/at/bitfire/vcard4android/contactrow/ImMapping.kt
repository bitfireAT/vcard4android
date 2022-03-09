/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.net.Uri
import at.bitfire.vcard4android.Constants
import at.bitfire.vcard4android.Utils.normalizeNFD
import org.apache.commons.lang3.StringUtils
import java.net.URI
import java.net.URISyntaxException
import java.util.logging.Level

object ImMapping {

    const val MESSENGER_AIM = "AIM"
    const val MESSENGER_FACEBOOK = "facebook"
    const val MESSENGER_ICQ = "ICQ"
    const val MESSENGER_IRC = "IRC"
    const val MESSENGER_QQ = "QQ"
    const val MESSENGER_SKYPE = "Skype"
    const val MESSENGER_TELEGRAM = "Telegram"
    const val MESSENGER_THREEMA = "Threema"
    const val MESSENGER_XMPP = "XMPP"

    const val SCHEME_AIM = "aim"
    const val SCHEME_ICQ = "icq"
    const val SCHEME_GOOGLE_TALK = "gtalk"
    const val SCHEME_HTTPS = "https"
    const val SCHEME_IRC = "irc"
    const val SCHEME_QQ = "mqq"
    const val SCHEME_SIP = "sip"
    const val SCHEME_SKYPE = "skype"
    const val SCHEME_XMPP = "xmpp"


    fun messengerToUri(messenger: String?, handle: String): URI? =
        try {
            when (messenger?.lowercase()) {
                null -> URI(null, handle, null)
                MESSENGER_AIM.lowercase() -> URI(SCHEME_AIM, handle, null)
                MESSENGER_FACEBOOK.lowercase() -> URI(SCHEME_HTTPS, "facebook.com", "/${handle}", null)
                MESSENGER_ICQ.lowercase() -> URI(SCHEME_HTTPS, "icq.im", "/${handle}", null)
                MESSENGER_IRC.lowercase() -> URI(SCHEME_IRC, handle, null)
                MESSENGER_QQ.lowercase() -> URI(SCHEME_QQ, "im", "/chat", "uin=${handle}", null)
                MESSENGER_SKYPE.lowercase() -> URI(SCHEME_SKYPE, handle, null)
                MESSENGER_TELEGRAM.lowercase() -> URI(SCHEME_HTTPS, "t.me", "/${handle}", null)
                MESSENGER_THREEMA.lowercase() -> URI(SCHEME_HTTPS, "threema.id", "/${handle}", null)
                MESSENGER_XMPP.lowercase() -> URI(SCHEME_XMPP, handle, null)
                else ->
                    // fallback for unknown messengers
                    URI(messengerToUriScheme(messenger), handle, null)
            }
        } catch (e: URISyntaxException) {
            Constants.log.log(Level.WARNING, "Couldn't generate URI from IM: $messenger / $handle", e)
            null
        }

    fun uriToMessenger(uri: URI, serviceType: String? = null): Pair<String?, String> =
        when {
            SCHEME_AIM.equals(uri.scheme, true) ->
                Pair(MESSENGER_AIM, uri.schemeSpecificPart)
            SCHEME_ICQ.equals(uri.scheme, true) ->
                Pair(MESSENGER_ICQ, uri.schemeSpecificPart)
            SCHEME_IRC.equals(uri.scheme, true) ->
                Pair(MESSENGER_IRC, uri.schemeSpecificPart)
            SCHEME_QQ.equals(uri.scheme, true) ||
            "qq".equals(uri.scheme, true) -> {
                val uri2 = Uri.parse(uri.toString())
                val uin =
                    try {
                        uri2.getQueryParameter("uin")
                    } catch (e: UnsupportedOperationException) {
                        null
                    }
                if (uin != null)
                    Pair(MESSENGER_QQ, uin)
                else
                    Pair(MESSENGER_QQ, uri.schemeSpecificPart)
            }
            SCHEME_SKYPE.equals(uri.scheme, true) ->
                Pair(MESSENGER_SKYPE, uri.schemeSpecificPart)
            SCHEME_XMPP.equals(uri.scheme, true) ->
                when (serviceType?.lowercase()) {
                    "facebook" -> Pair(MESSENGER_FACEBOOK, uri.schemeSpecificPart.removeSuffix("@facebook.com"))
                    else ->
                        Pair(MESSENGER_XMPP, uri.schemeSpecificPart)
                }

            "facebook.com".equals(uri.authority, true) ->
                Pair(MESSENGER_FACEBOOK, uri.path.trimStart('/'))
            "icq.im".equals(uri.authority, true) ->
                Pair(MESSENGER_ICQ, uri.path.trimStart('/'))
            "t.me".equals(uri.authority, true) ->
                Pair(MESSENGER_TELEGRAM, uri.path.trimStart('/'))
            "threema.id".equals(uri.authority, true) ->
                Pair(MESSENGER_THREEMA, uri.path.trimStart('/'))

            else -> {
                // fallback for unknown messengers
                val messenger: String? =
                    serviceType?.let { StringUtils.capitalize(it) } ?:  // use service type, if available
                    StringUtils.capitalize(uri.scheme)                  // otherwise, use the scheme itself
                Pair(messenger, uri.schemeSpecificPart)
            }
        }


    fun messengerToUriScheme(s: String?): String? {
        val reduced = s
            ?.normalizeNFD()     // normalize with decomposition first (e.g. Á → A+ ́)

                /* then filter according to RFC 3986 3.1:
               scheme      = ALPHA *( ALPHA / DIGIT / "+" / "-" / "." )
               ALPHA       =  %x41-5A / %x61-7A   ; A-Z / a-z
               DIGIT       =  %x30-39             ; 0-9
            */
            ?.replace(' ', '-')
            ?.replace(Regex("^[^a-zA-Z]+"), "")
            ?.replace(Regex("[^\\da-zA-Z+-.]"), "")
            ?.lowercase()
        return StringUtils.stripToNull(reduced)
    }

}
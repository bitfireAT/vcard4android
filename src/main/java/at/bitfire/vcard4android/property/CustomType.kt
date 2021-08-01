package at.bitfire.vcard4android.property

import ezvcard.parameter.RelatedType

object CustomType {

    val HOME = "home"
    val WORK = "work"

    object Nickname {
        val INITIALS = "x-initials"
        val MAIDEN_NAME = "x-maiden-name"
        val SHORT_NAME = "x-short-name"
    }

    object Related {
        val ASSISTANT = RelatedType.get("assistant")
        val BROTHER = RelatedType.get("brother")
        val DOMESTIC_PARTNER = RelatedType.get("domestic-partner")
        val FATHER = RelatedType.get("father")
        val MANAGER = RelatedType.get("manager")
        val MOTHER = RelatedType.get("mother")
        val PARTNER = RelatedType.get("partner")
        val REFERRED_BY = RelatedType.get("referred-by")
        val SISTER = RelatedType.get("sister")

        val OTHER = RelatedType.get("other")
    }

    object Url {
        const val TYPE_HOMEPAGE = "x-homepage"
        const val TYPE_BLOG = "x-blog"
        const val TYPE_PROFILE = "x-profile"
        const val TYPE_FTP = "x-ftp"
    }

}
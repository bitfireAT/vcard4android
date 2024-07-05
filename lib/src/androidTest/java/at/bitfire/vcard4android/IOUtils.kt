package at.bitfire.vcard4android

object IOUtils {
    fun resourceToByteArray(resource: String): ByteArray =
        this::class.java.getResourceAsStream(resource)!!.use { it.readBytes() }
}

package at.bitfire.vcard4android

object TestUtils {

    /**
     * Reads a resource from the classpath and returns it as a byte array.
     * @param resource The resource to read. Note that it must start with a `/`.
     * @throws NullPointerException If the resource doesn't exist.
     */
    fun resourceToByteArray(resource: String): ByteArray =
        this::class.java.getResourceAsStream(resource)!!.use { it.readBytes() }

}

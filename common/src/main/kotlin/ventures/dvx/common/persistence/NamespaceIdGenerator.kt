package ventures.dvx.common.persistence

import com.google.common.io.BaseEncoding
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.*

/***
 * ID Generator that is tighter than the UUID generator. It is expected that IDs start with the name space.
 *
 * For example:
 * String id = idGenerator.nextId("cus");
 * // id = cus_jgSiw9KgTimBITWDiN00qw
 *
 */
object NamespaceIdGenerator {

  fun nextId(namespace: String): String {
    return namespace + '_' + nextRawId()
  }

  fun getValueFromId(id: String): String {
    val base64 = id.substring(id.indexOf("_") + 1)
    val bytes: ByteArray = BaseEncoding.base64Url().decode(base64)
    return String(bytes, StandardCharsets.UTF_8)
  }

  private fun nextRawId(): String {
    val uuid = UUID.randomUUID()
    val byteBuffer = ByteBuffer.allocate(java.lang.Long.BYTES * 2)
    byteBuffer.putLong(uuid.mostSignificantBits)
    byteBuffer.putLong(uuid.leastSignificantBits)
    return BaseEncoding.base64Url().omitPadding().encode(byteBuffer.array())
  }

  private fun idWithValue(namespace: String, value: String): String {
    return namespace + '_' + BaseEncoding.base64Url().omitPadding().encode(value.toByteArray())
  }

}

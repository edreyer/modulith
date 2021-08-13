@file:Suppress("UnsupportedTypeWithoutConverterInspection")

package ventures.dvx.common.jpa

import org.springframework.data.util.ProxyUtils
import java.io.Serializable
import javax.persistence.Id
import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class AbstractJpaPersistable<T : Serializable>(@Id val id: T) {

  companion object {
    private val serialVersionUID = -5554308939380869754L
  }

  override fun equals(other: Any?): Boolean {
    other ?: return false

    if (this === other) return true

    if (javaClass != ProxyUtils.getUserClass(other)) return false

    other as AbstractJpaPersistable<*>

    return (this.id == other.id)
  }

  override fun hashCode(): Int {
    return 31
  }

  override fun toString() = "Entity ${this.javaClass.name} with id: $id"
}

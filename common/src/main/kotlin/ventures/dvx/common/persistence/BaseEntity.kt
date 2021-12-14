package ventures.dvx.common.persistence

import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime
import javax.persistence.Id
import javax.persistence.MappedSuperclass
import javax.persistence.PreRemove
import javax.persistence.Version

/**
 * This class inspired by: https://stackoverflow.com/questions/50233048/inherit-parent-properties-from-a-base-class-with-jpa-annotations-in-kotlin
 * Soft Delete strategy: https://www.baeldung.com/spring-jpa-soft-delete
 * JPA Audit strategy: https://www.baeldung.com/database-auditing-jpa
 */
@MappedSuperclass
abstract class BaseEntity(
  @Id private var id: String,
  @Transient private var namespace: String
) : Persistable<String> {

  init {
    if (!id.startsWith(namespace)) {
      throw IllegalStateException("$id must start with $namespace")
    }
  }

  @Version
  private var version: Long? = null

  @field:CreationTimestamp
  var createdAt: LocalDateTime? = null

  @field:UpdateTimestamp
  var updatedAt: LocalDateTime? = null

  var deletedAt: LocalDateTime? = null

  @PreRemove
  fun onPreRemove() {
    deletedAt = LocalDateTime.now()
  }

  override fun getId(): String {
    return id
  }

  override fun isNew(): Boolean {
    return version == null
  }

  override fun toString(): String {
    return "BaseEntity(id=$id, version=$version, createdAt=$createdAt, updatedAt=$updatedAt, isNew=$isNew)"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as BaseEntity
    if (id != other.id) return false
    return true
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }
}

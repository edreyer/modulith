package ventures.dvx.common.persistence

import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.annotations.Where
import org.springframework.data.domain.Persistable
import java.time.Instant
import javax.persistence.Id
import javax.persistence.MappedSuperclass
import javax.persistence.PrePersist
import javax.persistence.PreRemove
import javax.persistence.PreUpdate
import javax.persistence.Version

/**
 * Soft Delete strategy: https://www.baeldung.com/spring-jpa-soft-delete
 * JPA Audit strategy: https://www.baeldung.com/database-auditing-jpa
 */
@MappedSuperclass
@Where(clause = "deleted_at is null")
@FilterDef(name = "deletedProductFilter")
@Filter(name = "deletedProductFilter", condition = "deleted_at is not null")
abstract class BaseEntity(
  @Id private var id: String
) : Persistable<String> {

  @Version
  private var version: Long? = null

  @field:CreationTimestamp
  var createdAt: Instant? = null

  @field:UpdateTimestamp
  var updatedAt: Instant? = null

  @field:UpdateTimestamp
  var deletedAt: Instant? = null

  @PrePersist
  fun onPrePersist() {
    createdAt = createdAt ?: Instant.now()
  }

  @PreUpdate
  fun onPreUpdate() {
    updatedAt = Instant.now()
  }

  @PreRemove
  fun onPreRemove() {
    deletedAt = Instant.now()
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

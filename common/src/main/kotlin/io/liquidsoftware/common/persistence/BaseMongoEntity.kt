package io.liquidsoftware.common.persistence

import org.bson.types.ObjectId
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Transient
import org.springframework.data.annotation.Version
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

/**
 */
abstract class BaseMongoEntity(
  @Transient @JvmField var id: String,
  @Transient private var namespace: String,
  @Id private var mongoId: ObjectId? = null
) : Persistable<ObjectId> {

  init {
    if (!id.startsWith(namespace)) {
      throw IllegalStateException("$id must start with $namespace")
    }
  }

  @Version
  private var version: Long? = null

  @CreatedDate
  var createdAt: LocalDateTime? = null

  @CreatedBy
  var createdByUser: String? = null

  @LastModifiedDate
  var updatedAt: LocalDateTime? = null

  @LastModifiedBy
  var modifiedByUser: String? = null

  override fun getId(): ObjectId? = this.mongoId

  override fun isNew(): Boolean {
    return version == null
  }

  override fun toString(): String {
    return "BaseEntity(id=$id, mongoId=$mongoId, version=$version, createdAt=$createdAt, updatedAt=$updatedAt, createdBy=${createdByUser}, updatedBy=${modifiedByUser}, isNew=$isNew)"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as BaseMongoEntity
    if (id != other.id) return false
    return true
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }
}

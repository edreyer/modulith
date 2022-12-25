package io.liquidsoftware.base.user.adapter.out.persistence

import io.liquidsoftware.base.user.UserNamespace
import io.liquidsoftware.base.user.application.port.`in`.UserDisabledEvent
import io.liquidsoftware.base.user.application.port.`in`.UserEnabledEvent
import io.liquidsoftware.base.user.application.port.`in`.UserEvent
import io.liquidsoftware.base.user.domain.Role
import io.liquidsoftware.common.persistence.BaseMongoEntity
import io.liquidsoftware.common.security.acl.Acl
import io.liquidsoftware.common.security.acl.AclRole
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document("users")
internal class UserEntity(

  @Indexed(unique = true)
  var userId: String,

  @Indexed(unique = true)
  var msisdn: String,

  @Indexed(unique = true)
  var email: String,

  var password: String,

  var roles: MutableList<Role>,

  var active: Boolean = true

) : BaseMongoEntity(userId, UserNamespace.NAMESPACE) {

  fun acl() = Acl.of(userId, userId, AclRole.MANAGER)

  fun handle(event: UserEvent): UserEntity {
    return when(event) {
      is UserEnabledEvent -> handle(event)
      is UserDisabledEvent -> handle(event)
      else -> this
    }
  }

  private fun handle(event: UserEnabledEvent): UserEntity {
    this.active = true
    return this
  }

  private fun handle(event: UserDisabledEvent): UserEntity {
    this.active = false
    return this
  }

}

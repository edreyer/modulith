package ventures.dvx.base.user.adapter.out.persistence

import com.fasterxml.jackson.annotation.JsonInclude
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.hibernate.annotations.Where
import ventures.dvx.base.user.application.port.`in`.UserDisabledEvent
import ventures.dvx.base.user.application.port.`in`.UserEnabledEvent
import ventures.dvx.base.user.application.port.`in`.UserEvent
import ventures.dvx.base.user.domain.Role
import ventures.dvx.base.user.domain.UserNamespace
import ventures.dvx.common.persistence.BaseEntity
import ventures.dvx.common.persistence.NamespaceIdGenerator
import javax.persistence.CollectionTable
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.Table

@Entity
@Table(name = "users")
@Where(clause = "deleted_at is null")
@FilterDef(name = "deletedProductFilter")
@Filter(name = "deletedProductFilter", condition = "deleted_at is not null")
internal class UserEntity(

  var msisdn: String,

  var email: String,

  @Column(name = "encrypted_password")
  var password: String,

  @ElementCollection(fetch = FetchType.EAGER)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @Enumerated(EnumType.STRING)
  @CollectionTable(name = "user_roles", joinColumns = [JoinColumn(name = "user_id")])
  @Column(name = "role")
  var roles: MutableList<Role>,

  var active: Boolean = true

) : BaseEntity(NamespaceIdGenerator.nextId(UserNamespace.NAMESPACE)) {

  fun handle(event: UserEvent): UserEntity {
    return when(event) {
      is UserEnabledEvent -> handle(event)
      is UserDisabledEvent -> handle(event)
      else -> this
    }
  }

  fun handle(event: UserEnabledEvent): UserEntity {
    this.active = true
    return this
  }

  fun handle(event: UserDisabledEvent): UserEntity {
    this.active = false
    return this
  }

}

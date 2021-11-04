package ventures.dvx.base.user.adapter.out.persistence

import com.fasterxml.jackson.annotation.JsonInclude
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

enum class Role {
  ROLE_USER,
  ROLE_ADMIN
}

@Entity
@Table(name = "users")
class UserEntity(

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

) : BaseEntity(NamespaceIdGenerator.nextId(ID_NAMESPACE)) {

  companion object {
    private const val ID_NAMESPACE = "u"
  }

}

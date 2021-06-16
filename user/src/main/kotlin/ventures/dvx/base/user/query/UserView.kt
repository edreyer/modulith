package ventures.dvx.base.user.query

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.data.jpa.repository.JpaRepository
import ventures.dvx.base.user.command.UserRole
import java.util.*
import javax.persistence.CollectionTable
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.JoinColumn

@Entity
data class UserView(
  @Id
  val userId: UUID,
  val username: String,
  val password: String,
  val email: String,

  @ElementCollection(fetch = FetchType.EAGER)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @Enumerated(EnumType.STRING)
  @CollectionTable(name = "user_roles", joinColumns = [JoinColumn(name = "user_id")])
  @Column(name = "role")
  val roles: List<UserRole>,
)

interface UserViewRepository : JpaRepository<UserView, UUID> {
  fun findByUsername(username: String): UserView?
}

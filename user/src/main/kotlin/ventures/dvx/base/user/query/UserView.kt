package ventures.dvx.base.user.query

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class UserView(
  @Id
  val userId: UUID,
  val username: String,
  val email: String
)

interface UserViewRepository : JpaRepository<UserView, UUID> {
  fun findByUsername(username: String): UserView?
}

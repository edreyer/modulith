package ventures.dvx.base.user.adapter.out.persistence

enum class Role {
  ROLE_USER,
  ROLE_ADMIN
}

data class UserEntity(
  val email: String,
  val username: String,
  val password: String,
  val roles: List<Role>,
  val active: Boolean = true
)

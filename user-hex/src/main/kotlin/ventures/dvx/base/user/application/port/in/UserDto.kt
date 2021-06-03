package ventures.dvx.base.user.application.port.`in`

enum class RoleDto {
  ROLE_USER,
  ROLE_ADMIN
}

data class UserDto(
  val username: String,
  val password: String,
  val email: String,
  val active: Boolean,
  val roles: List<RoleDto>
)



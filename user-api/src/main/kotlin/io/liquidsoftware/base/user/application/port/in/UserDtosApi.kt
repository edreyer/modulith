package io.liquidsoftware.base.user.application.port.`in`

enum class RoleDto {
  ROLE_USER,
  ROLE_ADMIN
}

data class UserDto(
  val id: String,
  val email: String,
  val msisdn: String,
  val active: Boolean,
  val roles: List<RoleDto>
)

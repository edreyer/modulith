package ventures.dvx.base.user.application.port.`in`

import org.springframework.security.core.userdetails.UserDetails

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

data class UserDetailsDto(
  val userDetails: UserDetails,
  val id: String,
  val email: String,
  val msisdn: String,
  val active: Boolean,
  val roles: List<RoleDto>
) : UserDetails by userDetails


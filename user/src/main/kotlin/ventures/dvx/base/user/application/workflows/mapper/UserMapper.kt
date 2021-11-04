package ventures.dvx.base.user.application.workflows.mapper

import ventures.dvx.base.user.application.port.`in`.RoleDto
import ventures.dvx.base.user.application.port.`in`.UserDto
import ventures.dvx.base.user.domain.AdminUser
import ventures.dvx.base.user.domain.DisabledUser
import ventures.dvx.base.user.domain.User

fun User.toUserDto(): UserDto {
  val roles = when (this) {
    is AdminUser -> listOf(RoleDto.ROLE_ADMIN)
    else -> listOf(RoleDto.ROLE_USER)
  }
  return when (this) {
    is DisabledUser -> UserDto(
      username = this.msisdn.value,
      password = this.encryptedPassword.value,
      email = this.email.value,
      active = false,
      roles = roles
    )
    else -> UserDto(
      username = this.msisdn.value,
      password = this.encryptedPassword.value,
      email = this.email.value,
      active = true,
      roles = roles
    )
  }
}

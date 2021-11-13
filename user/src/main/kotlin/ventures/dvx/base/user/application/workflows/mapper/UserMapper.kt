package ventures.dvx.base.user.application.workflows.mapper

import ventures.dvx.base.user.application.port.`in`.RoleDto
import ventures.dvx.base.user.application.port.`in`.UserDto
import ventures.dvx.base.user.domain.AdminUser
import ventures.dvx.base.user.domain.DisabledUser
import ventures.dvx.base.user.domain.User

internal fun User.toUserDto(): UserDto {
  val roles = when (this) {
    is AdminUser -> listOf(RoleDto.ROLE_ADMIN)
    else -> listOf(RoleDto.ROLE_USER)
  }
  return when (this) {
    is DisabledUser -> UserDto(
      id = this.id.value,
      email = this.email.value,
      msisdn = this.msisdn.value,
      active = false,
      roles = roles
    )
    else -> UserDto(
      id = this.id.value,
      email = this.email.value,
      msisdn = this.msisdn.value,
      active = true,
      roles = roles
    )
  }
}


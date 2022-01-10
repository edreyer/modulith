package io.liquidsoftware.base.user.application.workflows.mapper

import io.liquidsoftware.base.user.application.port.`in`.RoleDto
import io.liquidsoftware.base.user.application.port.`in`.UserDto
import io.liquidsoftware.base.user.domain.AdminUser
import io.liquidsoftware.base.user.domain.DisabledUser
import io.liquidsoftware.base.user.domain.Role
import io.liquidsoftware.base.user.domain.UnregisteredUser
import io.liquidsoftware.base.user.domain.User

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

internal fun UnregisteredUser.toUserDto() =
  UserDto(
    id = "",
    email = this.email.value,
    msisdn = this.msisdn.value,
    active = true,
    roles = listOf(role.toRoleDto())
  )

internal fun Role.toRoleDto() = RoleDto.valueOf(name)

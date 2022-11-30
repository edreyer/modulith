package io.liquidsoftware.base.user.application.mapper

import io.liquidsoftware.base.user.application.port.`in`.RoleDto
import io.liquidsoftware.base.user.application.port.`in`.UserDto
import io.liquidsoftware.base.user.domain.AdminUser
import io.liquidsoftware.base.user.domain.DisabledUser
import io.liquidsoftware.base.user.domain.Role
import io.liquidsoftware.base.user.domain.UnregisteredUser
import io.liquidsoftware.base.user.domain.User

internal fun Role.toRoleDto(): RoleDto = RoleDto.valueOf(this.name)

internal fun User.toUserDto(): UserDto {
  val roles = when (this) {
    is AdminUser -> listOf(RoleDto.ROLE_ADMIN)
    else -> listOf(RoleDto.ROLE_USER)
  }
  return UserDto(
    id = this.id.value,
    email = this.email.value,
    msisdn = this.msisdn.value,
    active = this !is DisabledUser,
    roles = when (this) {
      is UnregisteredUser -> listOf(this.role.toRoleDto())
      is DisabledUser -> listOf(this.role.toRoleDto())
      else -> roles
    }
  )
}

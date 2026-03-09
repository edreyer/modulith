package io.liquidsoftware.base.user.adapter.out.module

import io.liquidsoftware.base.user.application.port.`in`.DisableUserCommand
import io.liquidsoftware.base.user.application.port.`in`.EnableUserCommand
import io.liquidsoftware.base.user.application.port.`in`.UserAdminApi
import io.liquidsoftware.common.context.ModuleApiRegistry

class LocalUserAdminApi : UserAdminApi {
  override suspend fun enableUser(command: EnableUserCommand) =
    ModuleApiRegistry.require(UserAdminApi::class).enableUser(command)

  override suspend fun disableUser(command: DisableUserCommand) =
    ModuleApiRegistry.require(UserAdminApi::class).disableUser(command)
}

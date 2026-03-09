package io.liquidsoftware.base.user.adapter.out.module

import io.liquidsoftware.base.user.application.port.`in`.RegisterUserApi
import io.liquidsoftware.base.user.application.port.`in`.RegisterUserCommand
import io.liquidsoftware.common.context.ModuleApiRegistry

class LocalRegisterUserApi : RegisterUserApi {
  override suspend fun registerUser(command: RegisterUserCommand) =
    ModuleApiRegistry.require(RegisterUserApi::class).registerUser(command)
}

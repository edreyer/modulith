package io.liquidsoftware.base.user.adapter.out.module

import io.liquidsoftware.base.user.application.port.`in`.SystemFindUserByEmailApi
import io.liquidsoftware.base.user.application.port.`in`.SystemFindUserByEmailQuery
import io.liquidsoftware.common.context.ModuleApiRegistry

class LocalSystemFindUserByEmailApi : SystemFindUserByEmailApi {
  override suspend fun findSystemUserByEmail(query: SystemFindUserByEmailQuery) =
    ModuleApiRegistry.require(SystemFindUserByEmailApi::class).findSystemUserByEmail(query)
}

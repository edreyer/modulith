package io.liquidsoftware.base.user.adapter.out.module

import io.liquidsoftware.base.user.application.port.`in`.FindUserApi
import io.liquidsoftware.base.user.application.port.`in`.FindUserByEmailQuery
import io.liquidsoftware.base.user.application.port.`in`.FindUserByIdQuery
import io.liquidsoftware.base.user.application.port.`in`.FindUserByMsisdnQuery
import io.liquidsoftware.common.context.ModuleApiRegistry

class LocalFindUserApi : FindUserApi {
  override suspend fun findUserById(query: FindUserByIdQuery) =
    ModuleApiRegistry.require(FindUserApi::class).findUserById(query)

  override suspend fun findUserByEmail(query: FindUserByEmailQuery) =
    ModuleApiRegistry.require(FindUserApi::class).findUserByEmail(query)

  override suspend fun findUserByMsisdn(query: FindUserByMsisdnQuery) =
    ModuleApiRegistry.require(FindUserApi::class).findUserByMsisdn(query)
}

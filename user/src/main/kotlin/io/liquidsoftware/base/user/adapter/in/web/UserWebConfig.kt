package io.liquidsoftware.base.user.adapter.`in`.web

import io.liquidsoftware.base.user.application.port.`in`.DisableUserCommand
import io.liquidsoftware.base.user.application.port.`in`.EnableUserCommand
import io.liquidsoftware.base.user.application.port.`in`.FindUserApi
import io.liquidsoftware.base.user.application.port.`in`.FindUserByEmailQuery
import io.liquidsoftware.base.user.application.port.`in`.FindUserByIdQuery
import io.liquidsoftware.base.user.application.port.`in`.FindUserByMsisdnQuery
import io.liquidsoftware.base.user.application.port.`in`.RegisterUserApi
import io.liquidsoftware.base.user.application.port.`in`.RegisterUserCommand
import io.liquidsoftware.base.user.application.port.`in`.SystemFindUserByEmailApi
import io.liquidsoftware.base.user.application.port.`in`.SystemFindUserByEmailQuery
import io.liquidsoftware.base.user.application.port.`in`.SystemUserFoundEvent
import io.liquidsoftware.base.user.application.port.`in`.UserAdminApi
import io.liquidsoftware.base.user.application.port.`in`.UserDisabledEvent
import io.liquidsoftware.base.user.application.port.`in`.UserEnabledEvent
import io.liquidsoftware.base.user.application.port.`in`.UserFoundEvent
import io.liquidsoftware.base.user.application.port.`in`.UserRegisteredEvent
import io.liquidsoftware.common.context.ModuleApiRegistry
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackages = ["io.liquidsoftware.base.user.adapter.in.web"])
class UserWebConfig {

  @Bean
  fun registerUserApi(): RegisterUserApi = object : RegisterUserApi {
    override suspend fun registerUser(command: RegisterUserCommand) =
      ModuleApiRegistry.require(RegisterUserApi::class).registerUser(command)
  }

  @Bean
  fun findUserApi(): FindUserApi = object : FindUserApi {
    override suspend fun findUserById(query: FindUserByIdQuery) =
      ModuleApiRegistry.require(FindUserApi::class).findUserById(query)

    override suspend fun findUserByEmail(query: FindUserByEmailQuery) =
      ModuleApiRegistry.require(FindUserApi::class).findUserByEmail(query)

    override suspend fun findUserByMsisdn(query: FindUserByMsisdnQuery) =
      ModuleApiRegistry.require(FindUserApi::class).findUserByMsisdn(query)
  }

  @Bean
  fun userAdminApi(): UserAdminApi = object : UserAdminApi {
    override suspend fun enableUser(command: EnableUserCommand) =
      ModuleApiRegistry.require(UserAdminApi::class).enableUser(command)

    override suspend fun disableUser(command: DisableUserCommand) =
      ModuleApiRegistry.require(UserAdminApi::class).disableUser(command)
  }

  @Bean
  fun systemFindUserByEmailApi(): SystemFindUserByEmailApi =
    object : SystemFindUserByEmailApi {
      override suspend fun findSystemUserByEmail(query: SystemFindUserByEmailQuery) =
        ModuleApiRegistry.require(SystemFindUserByEmailApi::class).findSystemUserByEmail(query)
    }
}

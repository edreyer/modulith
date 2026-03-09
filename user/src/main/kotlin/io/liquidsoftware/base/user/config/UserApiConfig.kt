package io.liquidsoftware.base.user.config

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
import io.liquidsoftware.base.user.application.port.`in`.UserAdminApi
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.base.user.application.port.out.UserEventPort
import io.liquidsoftware.base.user.application.workflows.DisableUserUseCase
import io.liquidsoftware.base.user.application.workflows.EnableUserUseCase
import io.liquidsoftware.base.user.application.workflows.FindUserByEmailUseCase
import io.liquidsoftware.base.user.application.workflows.FindUserByIdUseCase
import io.liquidsoftware.base.user.application.workflows.FindUserByMsisdnUseCase
import io.liquidsoftware.base.user.application.workflows.RegisterUserUseCase
import io.liquidsoftware.base.user.application.workflows.SystemFindUserByEmailUseCase
import io.liquidsoftware.common.context.ModuleApiRegistration
import io.liquidsoftware.common.context.ModuleApiRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class UserApiConfig {

  @Bean
  internal fun registerUserApiRegistration(
    passwordEncoder: PasswordEncoder,
    findUserPort: FindUserPort,
    userEventPort: UserEventPort,
  ): ModuleApiRegistration<RegisterUserApi> {
    val useCase = RegisterUserUseCase(passwordEncoder, findUserPort, userEventPort)
    return ModuleApiRegistry.register(RegisterUserApi::class, object : RegisterUserApi {
      override suspend fun registerUser(command: RegisterUserCommand) =
        useCase.execute(command)
    })
  }

  @Bean
  internal fun findUserApiRegistration(
    findUserPort: FindUserPort,
  ): ModuleApiRegistration<FindUserApi> {
    val findUserById = FindUserByIdUseCase(findUserPort)
    val findUserByEmail = FindUserByEmailUseCase(findUserPort)
    val findUserByMsisdn = FindUserByMsisdnUseCase(findUserPort)

    return ModuleApiRegistry.register(FindUserApi::class, object : FindUserApi {
      override suspend fun findUserById(query: FindUserByIdQuery) =
        findUserById.execute(query)

      override suspend fun findUserByEmail(query: FindUserByEmailQuery) =
        findUserByEmail.execute(query)

      override suspend fun findUserByMsisdn(query: FindUserByMsisdnQuery) =
        findUserByMsisdn.execute(query)
    })
  }

  @Bean
  internal fun userAdminApiRegistration(
    findUserPort: FindUserPort,
    userEventPort: UserEventPort,
  ): ModuleApiRegistration<UserAdminApi> {
    val enableUserUseCase = EnableUserUseCase(findUserPort, userEventPort)
    val disableUserUseCase = DisableUserUseCase(findUserPort, userEventPort)

    return ModuleApiRegistry.register(UserAdminApi::class, object : UserAdminApi {
      override suspend fun enableUser(command: EnableUserCommand) =
        enableUserUseCase.execute(command)

      override suspend fun disableUser(command: DisableUserCommand) =
        disableUserUseCase.execute(command)
    })
  }

  @Bean
  internal fun systemFindUserByEmailApiRegistration(
    findUserPort: FindUserPort,
  ): ModuleApiRegistration<SystemFindUserByEmailApi> {
    val useCase = SystemFindUserByEmailUseCase(findUserPort)
    return ModuleApiRegistry.register(SystemFindUserByEmailApi::class, object : SystemFindUserByEmailApi {
      override suspend fun findSystemUserByEmail(query: SystemFindUserByEmailQuery) =
        useCase.execute(query)
    })
  }
}

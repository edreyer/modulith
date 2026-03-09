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
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Bean

@Configuration
@ComponentScan(basePackages = ["io.liquidsoftware.base.user.adapter.in.web"])
class UserWebConfig {

  @Bean
  fun registerUserApi(dispatcher: WorkflowDispatcher): RegisterUserApi = object : RegisterUserApi {
    override suspend fun registerUser(command: RegisterUserCommand) =
      dispatcher.dispatch<UserRegisteredEvent>(command)
  }

  @Bean
  fun findUserApi(dispatcher: WorkflowDispatcher): FindUserApi = object : FindUserApi {
    override suspend fun findUserById(query: FindUserByIdQuery) =
      dispatcher.dispatch<UserFoundEvent>(query)

    override suspend fun findUserByEmail(query: FindUserByEmailQuery) =
      dispatcher.dispatch<UserFoundEvent>(query)

    override suspend fun findUserByMsisdn(query: FindUserByMsisdnQuery) =
      dispatcher.dispatch<UserFoundEvent>(query)
  }

  @Bean
  fun userAdminApi(dispatcher: WorkflowDispatcher): UserAdminApi = object : UserAdminApi {
    override suspend fun enableUser(command: EnableUserCommand) =
      dispatcher.dispatch<UserEnabledEvent>(command)

    override suspend fun disableUser(command: DisableUserCommand) =
      dispatcher.dispatch<UserDisabledEvent>(command)
  }

  @Bean
  fun systemFindUserByEmailApi(dispatcher: WorkflowDispatcher): SystemFindUserByEmailApi =
    object : SystemFindUserByEmailApi {
      override suspend fun findSystemUserByEmail(query: SystemFindUserByEmailQuery) =
        dispatcher.dispatch<SystemUserFoundEvent>(query)
    }
}

package ventures.dvx.base.user.query

import org.axonframework.eventhandling.EventHandler
import org.axonframework.messaging.InterceptorChain
import org.axonframework.messaging.annotation.MetaDataValue
import org.axonframework.messaging.interceptors.MessageHandlerInterceptor
import org.axonframework.queryhandling.QueryHandler
import org.axonframework.queryhandling.QueryMessage
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import ventures.dvx.base.user.api.AdminUserRegisteredEvent
import ventures.dvx.base.user.api.FindUserByIdQuery
import ventures.dvx.base.user.api.FindUserByUsernameQuery
import ventures.dvx.base.user.api.User
import ventures.dvx.base.user.api.UserNotFoundError
import ventures.dvx.base.user.api.UserRegistrationStartedEvent
import ventures.dvx.base.user.command.UserRole
import ventures.dvx.base.user.config.UserConfig.ResourceTypes.MY_USER
import ventures.dvx.base.user.config.UserConfig.ResourceTypes.NOT_MY_USER
import ventures.dvx.bridgekeeper.AccessControlQueryException
import ventures.dvx.bridgekeeper.BridgeKeeper
import ventures.dvx.bridgekeeper.Party
import ventures.dxv.base.user.error.UserException
import ventures.dxv.base.user.error.UserQueryErrorSupport

@Component
class UserProjector(
  private val userViewRepository: UserViewRepository
): UserQueryErrorSupport {

  @EventHandler
  fun on(event: UserRegistrationStartedEvent) {
    userViewRepository.save(
      UserView(event.userId.id, event.token.msisdn.value, "", event.token.email.value, listOf(UserRole.USER))
    )
  }

  @EventHandler
  fun on(event: AdminUserRegisteredEvent) {
    userViewRepository.save(
      UserView(event.userId.id, event.email.value, event.password.value, event.email.value, listOf(UserRole.ADMIN))
    )
  }

  @MessageHandlerInterceptor(
    payloadType = FindUserByUsernameQuery::class
  )
  fun checkFindUserByUsernameQuery(
    queryMsg: QueryMessage<*,*>,
    chain: InterceptorChain,
    bridgeKeeper: BridgeKeeper,
    @MetaDataValue("party") party: Party
  ): User {
    val user: User = chain.proceed() as User
    val userRt = if (party.id == user.id.toString())
      MY_USER else NOT_MY_USER
    bridgeKeeper.assertCanPerform(party, userRt, queryMsg.queryName)
      .orElseThrow { AccessControlQueryException(queryMsg.queryName, party.id) }
    return user
  }

  @QueryHandler
  fun handle(
    query: FindUserByUsernameQuery,
    passwordEncoder: PasswordEncoder
  ): User =
    userViewRepository.findByUsername(query.username)
      ?.let { User(
        id = it.id,
        username = it.username,
        password = it.password,
        email = it.email,
        roles = it.roles.map { role -> role.toString() }
      ) } ?: throw UserException(UserNotFoundError(query.username))

  @MessageHandlerInterceptor(
    payloadType = FindUserByIdQuery::class
  )
  fun checkFindUserByIdQuery(
    queryMsg: QueryMessage<*,*>,
    chain: InterceptorChain,
    bridgeKeeper: BridgeKeeper,
    @MetaDataValue("party") party: Party
  ): Any {
    val user: User = chain.proceed() as User
    val userRt = if (party.id == user.id.toString())
      MY_USER else NOT_MY_USER
    bridgeKeeper.assertCanPerform(party, userRt, queryMsg.queryName)
      .orElseThrow { AccessControlQueryException(queryMsg.queryName, party.id) }
    return user
  }

  @QueryHandler
  fun handle(
    query: FindUserByIdQuery,
    passwordEncoder: PasswordEncoder
  ): User =
    userViewRepository.findByIdOrNull(query.id)
      ?.let { User(
        id = it.id,
        username = it.username,
        password = it.password,
        email = it.email,
        roles = it.roles.map { role -> role.toString() }
      ) } ?: throw UserException(UserNotFoundError(query.id.toString()))
}

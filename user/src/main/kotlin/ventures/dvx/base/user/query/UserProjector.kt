package ventures.dvx.base.user.query

import org.axonframework.eventhandling.EventHandler
import org.axonframework.queryhandling.QueryHandler
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

  @QueryHandler
  fun handle(
    query: FindUserByUsernameQuery,
    passwordEncoder: PasswordEncoder
  ): User =
    userViewRepository.findByUsername(query.username)
      ?.let { User(
        id = it.userId,
        username = it.username,
        password = it.password,
        email = it.email,
        roles = it.roles.map { role -> role.toString() }
      ) } ?: throw UserException(UserNotFoundError(query.username))

  @QueryHandler
  fun handle(
    query: FindUserByIdQuery,
    passwordEncoder: PasswordEncoder
  ): User =
    userViewRepository.findByIdOrNull(query.id)
      ?.let { User(
        id = it.userId,
        username = it.username,
        password = it.password,
        email = it.email,
        roles = it.roles.map { role -> role.toString() }
      ) } ?: throw UserException(UserNotFoundError(query.id.toString()))

}

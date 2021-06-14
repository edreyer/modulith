package ventures.dvx.base.user.query

import org.axonframework.eventhandling.EventHandler
import org.axonframework.queryhandling.QueryHandler
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import ventures.dvx.base.user.api.FindUserQuery
import ventures.dvx.base.user.api.User
import ventures.dvx.base.user.api.UserRegistrationStartedEvent
import ventures.dvx.common.error.NotFoundException

@Component
class UserProjector(
  private val userViewRepository: UserViewRepository
) {

  @EventHandler
  fun on(event: UserRegistrationStartedEvent) {
    userViewRepository.save(UserView(event.userId.id, event.msisdn, event.email))
  }

  @QueryHandler
  fun handle(
    query: FindUserQuery,
    passwordEncoder: PasswordEncoder
  ): User =
    userViewRepository.findByUsername(query.username)
      ?.let { User(
        id = it.userId,
        username = it.username,
        email = it.email
      )
      } ?: throw NotFoundException("User with username ${query.username} not found")
}

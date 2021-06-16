package ventures.dvx.base.user.command

import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateCreationPolicy
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle.apply
import org.axonframework.modelling.command.CreationPolicy
import org.axonframework.spring.stereotype.Aggregate
import org.springframework.security.crypto.password.PasswordEncoder
import ventures.dvx.base.user.api.EndUserId
import ventures.dvx.base.user.api.RegisterEndUserCommand
import ventures.dvx.base.user.api.TokenValidatedEvent
import ventures.dvx.base.user.api.User
import ventures.dvx.base.user.api.UserRegistrationStartedEvent
import ventures.dvx.base.user.api.ValidateEndUserTokenCommand
import ventures.dvx.common.axon.IndexableAggregate
import ventures.dvx.common.axon.IndexableAggregateDto
import ventures.dvx.common.axon.command.persistence.IndexRepository
import ventures.dvx.common.error.ApplicationException
import java.time.Clock
import java.time.temporal.ChronoUnit

@Aggregate(cache = "userCache")
class EndUser() : BaseUser, IndexableAggregate() {

  @AggregateIdentifier
  private lateinit var id: EndUserId

  private lateinit var msisdn: String
  override lateinit var email :String
  override lateinit var firstName :String
  override lateinit var lastName :String

  override var roles : List<UserRole> = listOf(UserRole.USER)

  private lateinit var activeTokens: List<MsisdnToken>

  override val businessKey: String
    get() = msisdn

  @CommandHandler
  @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
  fun on(
    command: RegisterEndUserCommand,
    indexRepository: IndexRepository
  ): EndUserId {
    indexRepository.findEntityByAggregateNameAndKey(aggregateName, command.msisdn)
      ?.let { throw ApplicationException("User already exists with msisdn: ${command.msisdn}") }

    apply(
      UserRegistrationStartedEvent(
        ia = IndexableAggregateDto(aggregateName, command.msisdn),
        userId = command.userId,
        msisdn = command.msisdn,
        email = command.email,
        firstName = command.firstName,
        lastName = command.lastName
      )
    )

    return command.userId
  }

  @CommandHandler
  fun on(
    command: ValidateEndUserTokenCommand,
    passwordEncoder: PasswordEncoder
  ): User {
    val validToken = activeTokens
      .filter { it.isTokenValid() }
      .find { it.matches(command.token, command.msisdn) }
      ?: throw ApplicationException("Invalid token")

    apply(TokenValidatedEvent(validToken))

    val roles = this.roles.map { it.toString() }
    return User(id = id.id, username = msisdn, email = email, password = "", roles = roles)
  }

  @EventSourcingHandler
  private fun on(
    event: UserRegistrationStartedEvent,
    clock: Clock
  ) {
    id = event.userId

    val token = "1234" // TODO: plugin actual token generation mechanism
    activeTokens = listOf(MsisdnToken(
      token = token,
      msisdn = event.msisdn,
      email = event.email,
      expires = clock.instant().plus(1, ChronoUnit.HOURS)
    ))

    msisdn = event.msisdn
    email = event.email
    firstName = event.firstName
    lastName = event.lastName
  }

  @EventSourcingHandler
  private fun on(event: TokenValidatedEvent) {
    // delete the token we just used, and also clear out any expired tokens
    activeTokens = activeTokens
      .filter { it.isTokenValid() }
      .minus(event.msisdnToken)
  }

}

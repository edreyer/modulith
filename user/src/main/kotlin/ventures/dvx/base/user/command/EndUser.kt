package ventures.dvx.base.user.command

import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle.apply
import org.axonframework.spring.stereotype.Aggregate
import org.springframework.security.crypto.password.PasswordEncoder
import ventures.dvx.base.user.api.EndUserExistsError
import ventures.dvx.base.user.api.EndUserId
import ventures.dvx.base.user.api.EndUserLoginStartedEvent
import ventures.dvx.base.user.api.LoginEndUserCommand
import ventures.dvx.base.user.api.RegisterEndUserCommand
import ventures.dvx.base.user.api.TokenValidatedEvent
import ventures.dvx.base.user.api.User
import ventures.dvx.base.user.api.UserNotFoundError
import ventures.dvx.base.user.api.UserRegistrationStartedEvent
import ventures.dvx.base.user.api.ValidateEndUserTokenCommand
import ventures.dvx.common.axon.IndexableAggregate
import ventures.dvx.common.axon.IndexableAggregateDto
import ventures.dvx.common.axon.command.persistence.IndexRepository
import ventures.dvx.common.validation.MsisdnParser
import ventures.dxv.base.user.error.UserCommandErrorSupport
import ventures.dxv.base.user.error.UserException
import java.time.Clock
import java.time.temporal.ChronoUnit

@Aggregate(cache = "userCache")
class EndUser() : UserAggregate, UserCommandErrorSupport, IndexableAggregate {

  @AggregateIdentifier
  lateinit var id: EndUserId

  private lateinit var msisdn: String
  override lateinit var email :String
  override lateinit var firstName :String
  override lateinit var lastName :String

  override var roles : List<UserRole> = listOf(UserRole.USER)

  var token: MsisdnToken? = null

  override val businessKey: String
    get() = msisdn

  companion object {
    fun aggregateName() : String = EndUser::class.simpleName!!
  }

  @CommandHandler
  constructor(
    command: RegisterEndUserCommand,
    indexRepository: IndexRepository,
    msisdnParser: MsisdnParser
  ) : this() {
    indexRepository.findEntityByAggregateNameAndKey(aggregateName, command.msisdn)
      ?.let { throw UserException(EndUserExistsError(command.msisdn)) }

    apply(
      UserRegistrationStartedEvent(
        ia = IndexableAggregateDto(aggregateName, command.userId.id, command.msisdn),
        userId = command.userId,
        msisdn = msisdnParser.toInternational(command.msisdn), // throws if invalid (we want this here)
        email = command.email,
        firstName = command.firstName,
        lastName = command.lastName
      )
    )
  }

  @EventSourcingHandler
  private fun on(
    event: UserRegistrationStartedEvent,
    clock: Clock
  ) {
    id = event.userId

    val tokenStr = "1234" // TODO: plugin actual token generation mechanism
    token = MsisdnToken(
      token = tokenStr,
      msisdn = event.msisdn,
      email = event.email,
      expires = clock.instant().plus(1, ChronoUnit.HOURS)
    )

    msisdn = event.msisdn
    email = event.email
    firstName = event.firstName
    lastName = event.lastName
  }

  @CommandHandler
  fun on(
    command: LoginEndUserCommand,
    indexRepository: IndexRepository
  ) {
    // ensure user exists
    indexRepository.findEntityByAggregateNameAndKey(aggregateName, command.msisdn)
      ?: throw UserException(UserNotFoundError(command.msisdn))

    apply(EndUserLoginStartedEvent())
  }

  @EventSourcingHandler
  private fun on(event: EndUserLoginStartedEvent, clock: Clock) {
    val tokenStr = "1234" // TODO: plugin actual token generation mechanism
    token = MsisdnToken(
      token = tokenStr,
      msisdn = msisdn,
      email = email,
      expires = clock.instant().plus(1, ChronoUnit.HOURS)
    )
  }

  @CommandHandler
  fun on(
    command: ValidateEndUserTokenCommand,
    passwordEncoder: PasswordEncoder
  ): User {
    token
      ?.takeIf { it.isTokenValid() }
      ?.takeIf { it.matches(command.token, command.msisdn) }
      ?.run { apply(TokenValidatedEvent()) }

    val roles = this.roles.map { it.toString() }
    return User(id = id.id, username = msisdn, email = email, password = "", roles = roles)
  }

  @EventSourcingHandler
  private fun on(event: TokenValidatedEvent) {
    // delete the token we just used
    token = null
  }
}

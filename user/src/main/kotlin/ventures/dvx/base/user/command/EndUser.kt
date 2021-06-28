package ventures.dvx.base.user.command

import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateCreationPolicy
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle.apply
import org.axonframework.modelling.command.CreationPolicy
import org.axonframework.spring.stereotype.Aggregate
import org.springframework.security.crypto.password.PasswordEncoder
import ventures.dvx.base.user.api.EndUserExistsError
import ventures.dvx.base.user.api.EndUserId
import ventures.dvx.base.user.api.EndUserLoginStartedEvent
import ventures.dvx.base.user.api.InvalidTokenError
import ventures.dvx.base.user.api.LoginEndUserCommand
import ventures.dvx.base.user.api.RegisterEndUserCommand
import ventures.dvx.base.user.api.TokenValidatedEvent
import ventures.dvx.base.user.api.User
import ventures.dvx.base.user.api.UserNotFoundError
import ventures.dvx.base.user.api.UserRegistrationStartedEvent
import ventures.dvx.base.user.api.ValidateEndUserTokenCommand
import ventures.dvx.base.user.config.UserConfig
import ventures.dvx.common.axon.IndexableAggregate
import ventures.dvx.common.axon.IndexableAggregateDto
import ventures.dvx.common.axon.command.persistence.IndexRepository
import ventures.dvx.common.config.CommonConfig
import ventures.dvx.common.validation.MsisdnParser
import ventures.dxv.base.user.error.UserCommandErrorSupport
import ventures.dxv.base.user.error.UserException
import java.time.Clock
import java.time.temporal.ChronoUnit

@Aggregate(
  snapshotTriggerDefinition = "endUserSnapshotTriggerDefinition",
  cache = "userCache"
)
class EndUser : UserAggregate, UserCommandErrorSupport, IndexableAggregate {

  @AggregateIdentifier
  lateinit var id: EndUserId

  private lateinit var msisdn: String
  override lateinit var email: String
  override lateinit var firstName: String
  override lateinit var lastName: String

  override var roles : List<UserRole> = listOf(UserRole.USER)

  var token: MsisdnToken? = null

  override val businessKey: String
    get() = msisdn

  companion object {
    fun aggregateName() : String = EndUser::class.simpleName!!
  }

  @CommandHandler
  @CreationPolicy(AggregateCreationPolicy.ALWAYS)
  fun handle(
    command: RegisterEndUserCommand,
    commonConfig: CommonConfig,
    userConfig: UserConfig,
    indexRepository: IndexRepository,
    msisdnParser: MsisdnParser,
    clock: Clock
  ) {
    indexRepository.findEntityByAggregateNameAndKey(aggregateName, command.msisdn)
      ?.let { throw UserException(EndUserExistsError(command.msisdn)) }

    val token = createToken(
      commonConfig, userConfig, clock, msisdnParser.toInternational(command.msisdn), command.email
    )

    apply(
      UserRegistrationStartedEvent(
        ia = IndexableAggregateDto(aggregateName, command.userId.id, command.msisdn),
        token = token,
        userId = command.userId,
        firstName = command.firstName,
        lastName = command.lastName
      )
    )
  }

  @EventSourcingHandler
  private fun on(
    event: UserRegistrationStartedEvent
  ) {
    id = event.userId
    token = event.token
    msisdn = event.token.msisdn
    email = event.token.email
    firstName = event.firstName
    lastName = event.lastName
  }

  @CommandHandler
  fun handle(
    command: LoginEndUserCommand,
    indexRepository: IndexRepository,
    msisdnParser: MsisdnParser,
    commonConfig: CommonConfig,
    userConfig: UserConfig,
    clock: Clock
  ): EndUserId {
    // ensure user exists
    indexRepository.findEntityByAggregateNameAndKey(aggregateName, command.msisdn)
      ?: throw UserException(UserNotFoundError(command.msisdn))

    apply(EndUserLoginStartedEvent(
      createToken(
        commonConfig, userConfig, clock, msisdnParser.toInternational(command.msisdn), email
      )
    ))

    return id
  }

  @EventSourcingHandler
  private fun on(event: EndUserLoginStartedEvent) {
    token = event.token
  }

  @CommandHandler
  fun handle(
    command: ValidateEndUserTokenCommand,
    passwordEncoder: PasswordEncoder
  ): User = token
    ?.takeIf { it.isTokenValid() }
    ?.takeIf { it.matches(command.token, command.msisdn) }
    ?.apply { apply(TokenValidatedEvent()) }
    ?.let {
      val roles = this.roles.map { it.toString() }
      return User(id = id.id, username = msisdn, email = email, password = "", roles = roles)
    } ?: throw UserException(InvalidTokenError)

  @EventSourcingHandler
  private fun on(event: TokenValidatedEvent) {
    // delete the token we just used
    token = null
  }

  private fun createToken(
    commonConfig: CommonConfig,
    userConfig: UserConfig,
    clock: Clock,
    msisdn: String,
    email: String,
  ): MsisdnToken {
    val tokenStr = if (commonConfig.isDev()) userConfig.forcedMsisdnToken else MsisdnToken.generateToken()
    return MsisdnToken(
      token = tokenStr,
      msisdn = msisdn,
      email = email,
      expires = clock.instant().plus(1, ChronoUnit.HOURS)
    )
  }
}

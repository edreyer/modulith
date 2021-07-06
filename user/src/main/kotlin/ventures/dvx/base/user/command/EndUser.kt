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
import ventures.dvx.base.user.config.UserConfig.ResourceTypes
import ventures.dvx.bridgekeeper.AccessControlCommandSupport
import ventures.dvx.bridgekeeper.Party
import ventures.dvx.bridgekeeper.ResourceType
import ventures.dvx.common.axon.IndexableAggregate
import ventures.dvx.common.axon.IndexableAggregateDto
import ventures.dvx.common.axon.command.persistence.IndexRepository
import ventures.dvx.common.config.CommonConfig
import ventures.dvx.common.types.EmailAddress
import ventures.dvx.common.types.Msisdn
import ventures.dvx.common.types.NonEmptyString
import ventures.dvx.common.validation.MsisdnParser
import ventures.dxv.base.user.error.UserCommandErrorSupport
import ventures.dxv.base.user.error.UserException
import java.time.Clock
import java.time.temporal.ChronoUnit

@Aggregate(
  snapshotTriggerDefinition = "endUserSnapshotTriggerDefinition",
  cache = "userCache"
)
class EndUser : UserAggregate, AccessControlCommandSupport, UserCommandErrorSupport, IndexableAggregate {

  @AggregateIdentifier
  lateinit var id: EndUserId

  private lateinit var msisdn: Msisdn
  override lateinit var email: EmailAddress
  override lateinit var firstName: NonEmptyString
  override lateinit var lastName: NonEmptyString

  override var roles : List<UserRole> = listOf(UserRole.USER)

  var token: MsisdnToken? = null

  override val businessKey: String
    get() = msisdn.toString()

  override fun getId() = id.id.toString()

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
  ): EndUserId {
    indexRepository.findEntityByAggregateNameAndKey(aggregateName, command.msisdn.value)
      ?.let { throw UserException(EndUserExistsError(command.msisdn)) }

    val token = createToken(
      commonConfig, userConfig, clock, msisdnParser.toInternational(command.msisdn), command.email
    )

    apply(
      UserRegistrationStartedEvent(
        ia = IndexableAggregateDto(aggregateName, command.userId.id, command.msisdn.value),
        token = token,
        userId = command.userId,
        firstName = command.firstName,
        lastName = command.lastName
      )
    )

    return command.userId
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
    indexRepository.findEntityByAggregateNameAndKey(aggregateName, command.msisdn.value)
      ?: throw UserException(UserNotFoundError(command.msisdn.value))

    apply(EndUserLoginStartedEvent(
      createToken(
        commonConfig, userConfig, clock, msisdnParser.toInternational(command.msisdn), email
      )
    ))

    return id
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
      return User(id = id!!.id, username = msisdn.value, email = email.value, password = "", roles = roles)
    } ?: throw UserException(InvalidTokenError)

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

  @EventSourcingHandler
  private fun on(event: EndUserLoginStartedEvent) {
    token = event.token
  }

  @EventSourcingHandler
  private fun on(event: TokenValidatedEvent) {
    // delete the token we just used
    token = null
  }

  override fun establishResourceType(party: Party): ResourceType =
    when (party.id == this.id.id.toString()) {
      true -> ResourceTypes.MY_USER
      false -> ResourceTypes.NOT_MY_USER
    }

  private fun createToken(
    commonConfig: CommonConfig,
    userConfig: UserConfig,
    clock: Clock,
    msisdn: Msisdn,
    email: EmailAddress,
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

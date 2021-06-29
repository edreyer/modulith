package ventures.dvx.base.user.command

import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.messaging.annotation.MetaDataValue
import org.axonframework.modelling.command.AggregateCreationPolicy
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle.apply
import org.axonframework.modelling.command.CommandHandlerInterceptor
import org.axonframework.modelling.command.CreationPolicy
import org.axonframework.spring.stereotype.Aggregate
import org.springframework.security.crypto.password.PasswordEncoder
import ventures.dvx.base.user.api.*
import ventures.dvx.bridgekeeper.BridgeKeeper
import ventures.dvx.bridgekeeper.PartyContext
import ventures.dvx.bridgekeeper.ResourceType
import ventures.dvx.common.axon.IndexableAggregate
import ventures.dvx.common.axon.IndexableAggregateDto
import ventures.dvx.common.axon.command.persistence.IndexRepository
import ventures.dvx.common.types.EmailAddress
import ventures.dvx.common.types.NonEmptyString
import ventures.dvx.common.types.getOrThrow
import ventures.dxv.base.user.error.UserCommandErrorSupport
import ventures.dxv.base.user.error.UserException

@Aggregate(
  snapshotTriggerDefinition = "adminUserSnapshotTriggerDefinition",
  cache = "userCache")
class AdminUser: UserAggregate, UserCommandErrorSupport, IndexableAggregate {

  @AggregateIdentifier
  var id: AdminUserId? = null

  lateinit var password: NonEmptyString // encrypted
  override lateinit var email: EmailAddress
  override lateinit var firstName :NonEmptyString
  override lateinit var lastName :NonEmptyString

  override var roles : List<UserRole> = listOf(UserRole.ADMIN)

  override val businessKey: String
    get() = email.toString()

  companion object {
    fun aggregateName() : String = AdminUser::class.simpleName!!
    object ADMIN: ResourceType()
  }

  @CommandHandler
  @CreationPolicy(AggregateCreationPolicy.ALWAYS)
  fun handle(
    command: RegisterAdminUserCommand,
    indexRepository: IndexRepository,
    passwordEncoder: PasswordEncoder
  ) {
    indexRepository.findEntityByAggregateNameAndKey(aggregateName, command.email.value)
      ?.let { throw UserException(AdminUserExistsError(command.email)) }

    apply(
      AdminUserRegisteredEvent(
        ia = IndexableAggregateDto(aggregateName, command.userId.id, command.email.value),
        userId = command.userId,
        password = command.plainPassword
          .let { passwordEncoder.encode(it.value) }
          .let { NonEmptyString.of(it).getOrThrow() },
        email = command.email,
        firstName = command.firstName,
        lastName = command.lastName
      )
    )
  }


  @CommandHandlerInterceptor
  fun checkAuthorization(cmdCandidate: SecuredCommand, @MetaDataValue("partyContext") partyContext: PartyContext) {
    val userRt = establishAdminUserType(partyContext)
    val bridgeKeeper = getBridgeKeeper() //todo replace with injection
    bridgeKeeper.assertCanPerform(partyContext.authenticatedParty, userRt, cmdCandidate)
        .orElseThrow {
          UserException(
            UnauthorizedCommand(
              partyContext.authenticatedParty.id,
              cmdCandidate,
              this.id?.toString()
            )
          )
        }
  }

  private fun getBridgeKeeper(): BridgeKeeper = TODO()

  fun establishAdminUserType(partyContext: PartyContext): ResourceType {
    return ADMIN
  }




  @EventSourcingHandler
  private fun on(event: AdminUserRegisteredEvent) {
    id = event.userId
    email = event.email
    password = event.password
    firstName = event.firstName
    lastName = event.lastName
  }
}

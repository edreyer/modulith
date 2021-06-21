package ventures.dvx.base.user.command

import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle.apply
import org.axonframework.spring.stereotype.Aggregate
import org.springframework.security.crypto.password.PasswordEncoder
import ventures.dvx.base.user.api.AdminUserExistsError
import ventures.dvx.base.user.api.AdminUserId
import ventures.dvx.base.user.api.AdminUserRegisteredEvent
import ventures.dvx.base.user.api.RegisterAdminUserCommand
import ventures.dvx.common.axon.IndexableAggregate
import ventures.dvx.common.axon.IndexableAggregateDto
import ventures.dvx.common.axon.command.persistence.IndexRepository
import ventures.dxv.base.user.error.UserCommandErrorSupport
import ventures.dxv.base.user.error.UserException

@Aggregate(cache = "userCache")
class AdminUser(): UserAggregate, UserCommandErrorSupport, IndexableAggregate {

  @AggregateIdentifier
  lateinit var id: AdminUserId

  lateinit var password: String // encrypted
  override lateinit var email :String
  override lateinit var firstName :String
  override lateinit var lastName :String

  override var roles : List<UserRole> = listOf(UserRole.ADMIN)

  override val businessKey: String
    get() = email

  companion object {
    fun aggregateName() : String = AdminUser::class.simpleName!!
  }

  @CommandHandler
  constructor(
    command: RegisterAdminUserCommand,
    indexRepository: IndexRepository,
    passwordEncoder: PasswordEncoder
  ) : this() {
    indexRepository.findEntityByAggregateNameAndKey(aggregateName, command.email)
      ?.let { throw UserException(AdminUserExistsError(command.email)) }

    apply(
      AdminUserRegisteredEvent(
        ia = IndexableAggregateDto(aggregateName, command.userId.id, command.email),
        userId = command.userId,
        password = passwordEncoder.encode(command.plainPassword),
        email = command.email,
        firstName = command.firstName,
        lastName = command.lastName
      )
    )
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

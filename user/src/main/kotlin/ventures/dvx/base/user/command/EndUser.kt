package ventures.dvx.base.user.command

import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventhandling.EventHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle.apply
import org.axonframework.spring.stereotype.Aggregate
import ventures.dvx.base.user.api.RegisterUserCommand
import ventures.dvx.base.user.api.UserRegistrationStartedEvent
import ventures.dvx.base.user.command.persistence.IndexEntity
import ventures.dvx.base.user.command.persistence.IndexRepository
import ventures.dvx.common.axon.WithKey
import java.util.*

//@Profile("command")
@Aggregate(cache = "userCache")
class EndUser : WithKey {

  @AggregateIdentifier
  private lateinit var userId: UUID
  private lateinit var email :String
  private lateinit var firstName :String
  private lateinit var lastName :String

  override fun businessKey(): String = firstName + lastName + email;

  @CommandHandler
  fun handle(
    command: RegisterUserCommand,
    indexRepository: IndexRepository
  ) {
    indexRepository.findEntityByAggregateNameAndKey(this.javaClass.simpleName, businessKey())
      ?.let { throw IllegalStateException("User already exists with phone: ${command.msisdn}") }

    apply(UserRegistrationStartedEvent(
      userId = UUID.randomUUID(),
      msisdn = command.msisdn,
      email = command.email,
      firstName = command.firstName,
      lastName = command.lastName
      ))
  }

  // TODO: Should I just save the index entity in the @CommandHandler method?
  @EventHandler
  fun on(event: UserRegistrationStartedEvent, indexRepository: IndexRepository) {
    indexRepository.save(IndexEntity(
      UUID.randomUUID(),
      this.javaClass.simpleName,
      businessKey()
    ))
  }

  @EventSourcingHandler
  fun on(event: UserRegistrationStartedEvent): UUID {
    userId = event.userId
    email = event.email
    firstName = event.firstName
    lastName = event.lastName
    return userId
  }
}

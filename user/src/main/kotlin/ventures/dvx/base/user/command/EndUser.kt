package ventures.dvx.base.user.command

import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle.apply
import org.axonframework.spring.stereotype.Aggregate
import ventures.dvx.base.user.api.RegisterUserCommand
import ventures.dvx.base.user.api.UserRegistrationStartedEvent
import ventures.dvx.common.axon.IndexableAggregate
import ventures.dvx.common.axon.IndexableAggregateDto
import ventures.dvx.common.axon.command.persistence.IndexRepository
import java.util.*

@Aggregate(cache = "userCache")
class EndUser() : IndexableAggregate {

  @AggregateIdentifier
  private lateinit var userId: UUID
  private lateinit var msisdn: String
  private lateinit var email :String
  private lateinit var firstName :String
  private lateinit var lastName :String

  override val businessKey: String
    get() = msisdn

  @CommandHandler
  constructor(
    command: RegisterUserCommand,
    indexRepository: IndexRepository
  ) : this() {
    indexRepository.findEntityByAggregateNameAndKey(aggregateName, command.msisdn)
      ?.let { throw IllegalStateException("User already exists with msisdn: ${command.msisdn}") }

    apply(UserRegistrationStartedEvent(
      ia = IndexableAggregateDto(aggregateName, command.msisdn),
      userId = command.id,
      msisdn = command.msisdn,
      email = command.email,
      firstName = command.firstName,
      lastName = command.lastName
    ))
  }

  @EventSourcingHandler
  fun on(event: UserRegistrationStartedEvent): UUID {
    userId = event.userId
    msisdn = event.msisdn
    email = event.email
    firstName = event.firstName
    lastName = event.lastName
    return userId
  }
}

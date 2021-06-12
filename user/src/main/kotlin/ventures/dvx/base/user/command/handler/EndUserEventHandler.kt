package ventures.dvx.base.user.command.handler

import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.eventhandling.EventHandler
import org.springframework.stereotype.Component
import ventures.dvx.base.user.api.CreateTokenCommand
import ventures.dvx.base.user.api.UserRegistrationStartedEvent
import ventures.dvx.common.axon.command.persistence.IndexJpaEntity
import ventures.dvx.common.axon.command.persistence.IndexRepository

@Component
class EndUserEventHandler(
  private val commandGateway: CommandGateway
) {

  @EventHandler
  private fun on(event: UserRegistrationStartedEvent, indexRepository: IndexRepository) {
    indexRepository.save(
      IndexJpaEntity(
        aggregateName = event.ia.aggregateName,
        key = event.ia.businessKey
      )
    )

    commandGateway.send<Unit>(CreateTokenCommand(
      userId = event.userId,
      msisdn = event.msisdn,
      email = event.email
    ))
  }

}

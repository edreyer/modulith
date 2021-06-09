package ventures.dvx.base.user.command.handler

import org.axonframework.eventhandling.EventHandler
import org.springframework.stereotype.Component
import ventures.dvx.base.user.api.UserRegistrationStartedEvent
import ventures.dvx.base.user.command.persistence.IndexEntity
import ventures.dvx.base.user.command.persistence.IndexRepository
import java.util.*

@Component
class EndUserEventHandler {

  @EventHandler
  fun on(event: UserRegistrationStartedEvent, indexRepository: IndexRepository) {
    indexRepository.save(
      IndexEntity(
        UUID.randomUUID(),
        this.javaClass.simpleName,
        event.msisdn
      )
    )
  }

}

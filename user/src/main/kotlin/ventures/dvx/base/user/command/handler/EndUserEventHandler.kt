package ventures.dvx.base.user.command.handler

import org.axonframework.eventhandling.EventHandler
import org.axonframework.messaging.interceptors.ExceptionHandler
import org.springframework.stereotype.Component
import ventures.dvx.base.user.api.UserRegistrationStartedEvent
import ventures.dvx.common.axon.command.persistence.IndexJpaEntity
import ventures.dvx.common.axon.command.persistence.IndexRepository
import ventures.dvx.common.error.PreconditionFailedCommandException

@Component
class EndUserEventHandler {

  @EventHandler
  private fun on(
    event: UserRegistrationStartedEvent,
    indexRepository: IndexRepository
  ) {
    indexRepository.save(
      IndexJpaEntity(
        aggregateName = event.ia.aggregateName,
        key = event.ia.businessKey,
      )
    )
  }

  // TODO Why doesn't this get called?
  @ExceptionHandler
  fun handle(ex: Exception) {
    val msg = ex.message  ?: "Unknown Error"
    throw PreconditionFailedCommandException(msg, ex, msg)
  }

}

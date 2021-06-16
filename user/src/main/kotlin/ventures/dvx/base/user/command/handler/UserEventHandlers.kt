package ventures.dvx.base.user.command.handler

import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.axonframework.messaging.interceptors.ExceptionHandler
import org.springframework.stereotype.Component
import ventures.dvx.common.axon.IndexableAggregateEvent
import ventures.dvx.common.axon.command.persistence.IndexJpaEntity
import ventures.dvx.common.axon.command.persistence.IndexRepository
import ventures.dvx.common.error.PreconditionFailedCommandException
import ventures.dvx.common.logging.LoggerDelegate
import javax.annotation.PostConstruct

@Component
@ProcessingGroup("indexEntity")
class UserEventHandlers {

  val log by LoggerDelegate()

  @PostConstruct
  fun init() {
    log.debug("Initialized UserEventHandlers")
  }

  @EventHandler
  fun on(
    event: IndexableAggregateEvent,
    indexRepository: IndexRepository
  ) {
    indexRepository.save(
      IndexJpaEntity(
        aggregateName = event.ia.aggregateName,
        aggregateId = event.ia.aggregateId,
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

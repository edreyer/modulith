package ventures.dvx.base.user.command.handler

import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.axonframework.messaging.interceptors.ExceptionHandler
import org.springframework.stereotype.Component
import ventures.dvx.common.axon.IndexableAggregateEvent
import ventures.dvx.common.axon.command.persistence.IndexJpaEntity
import ventures.dvx.common.axon.command.persistence.IndexRepository
import ventures.dvx.common.logging.LoggerDelegate
import ventures.dxv.base.user.error.UserCommandException
import ventures.dxv.base.user.error.UserException
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

  // Doesn't get called on aggregate creation: https://github.com/AxonFramework/AxonFramework/issues/1850
  @ExceptionHandler(resultType = UserException::class)
  fun handle(ex: UserException) {
    throw UserCommandException(ex.userError.msg, ex.userError, ex)
  }

}

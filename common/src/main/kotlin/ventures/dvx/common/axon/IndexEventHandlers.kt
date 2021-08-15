package ventures.dvx.common.axon

import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.springframework.stereotype.Component
import ventures.dvx.common.axon.command.persistence.IndexJpaEntity
import ventures.dvx.common.axon.command.persistence.IndexRepository
import ventures.dvx.common.logging.LoggerDelegate
import javax.annotation.PostConstruct

@Component
@ProcessingGroup("indexEntity")
class IndexEventHandlers {

  val log by LoggerDelegate()

  @PostConstruct
  fun init() {
    log.debug("Initialized IndexEventHandlers")
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

}

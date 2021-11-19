package ventures.dvx.common.events

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import ventures.dvx.common.logging.LoggerDelegate
import ventures.dvx.common.workflow.Event

@Component
class SimpleEventPublisher(
  private val appEventPublisher: ApplicationEventPublisher
) : EventPublisher {

  val log by LoggerDelegate()

  override fun <T: Event> publish(event: T): T {
    log.info("Application Event: ${event}")
    appEventPublisher.publishEvent(event)
    return event
  }
}

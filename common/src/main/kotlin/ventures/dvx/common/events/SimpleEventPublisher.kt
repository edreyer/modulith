package ventures.dvx.common.events

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import ventures.dvx.common.workflow.Event

@Component
class SimpleEventPublisher(
  private val appEventPublisher: ApplicationEventPublisher
) : EventPublisher {
  override fun <T: Event> publish(event: T): T {
    appEventPublisher.publishEvent(event)
    return event
  }
}

package ventures.dvx.common.events

import ventures.dvx.common.workflow.Event

interface EventPublisher {

  fun <T: Event> publish(event: T): T

  fun <T: Event> publish(events: List<T>) = events.map { publish(it) }

}

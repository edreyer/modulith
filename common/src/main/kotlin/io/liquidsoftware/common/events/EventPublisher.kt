package io.liquidsoftware.common.events

import io.liquidsoftware.common.workflow.Event

interface EventPublisher {

  fun <T: Event> publish(event: T): T

  fun <T: Event> publish(events: List<T>): List<T> = events.map { publish(it) }

}

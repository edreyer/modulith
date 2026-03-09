package io.liquidsoftware.common.workflow

import java.time.Instant
import java.util.UUID

sealed interface Request
interface Command : Request
interface Query : Request

abstract class WorkflowError(override val message: String) : RuntimeException(message)

abstract class Event {
  val eventId: UUID = UUID.randomUUID()
  val instant: Instant = Instant.now()
}

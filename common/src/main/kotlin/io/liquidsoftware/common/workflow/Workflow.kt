package io.liquidsoftware.common.workflow

import io.liquidsoftware.common.ext.className
import io.liquidsoftware.common.logging.LoggerDelegate
import java.time.Instant
import java.util.UUID

// Workflow Input type
sealed interface Request
interface Command : Request
interface Query : Request

// Workflow Output type
abstract class Event {
  val eventId: UUID = UUID.randomUUID()
  val instant: Instant = Instant.now()
}

interface Workflow<E : Event>

interface SafeWorkflow<R: Request, E : Event> : Workflow<E> {
  suspend operator fun invoke(request: R): E
}

abstract class BaseSafeWorkflow<R: Request, E : Event> : SafeWorkflow<R, E> {
  private val log by LoggerDelegate()

  final override suspend operator fun invoke(request: R): E {
    log.debug("Executing workflow ${this.className()} with request $request")
    return execute(request)
  }

  abstract suspend fun execute(request: R): E
}



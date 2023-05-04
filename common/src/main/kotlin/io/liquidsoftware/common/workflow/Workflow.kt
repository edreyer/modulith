package io.liquidsoftware.common.workflow

import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import io.liquidsoftware.common.ext.className
import io.liquidsoftware.common.logging.LoggerDelegate
import io.liquidsoftware.common.types.ValidationException
import java.time.Instant
import java.util.UUID

// Workflow Input type
sealed interface Request
interface Command : Request
interface Query : Request

abstract class WorkflowError(override val message: String) : RuntimeException(message)

// Workflow Output type
abstract class Event {
  val eventId: UUID = UUID.randomUUID()
  val instant: Instant = Instant.now()
}

interface Workflow<E : Event>

interface SafeWorkflow<R: Request, E : Event> : Workflow<E> {
  context(Raise<WorkflowError>)
  suspend fun invoke(request: R): E
}

abstract class BaseSafeWorkflow<R: Request, E : Event> : SafeWorkflow<R, E> {
  private val log by LoggerDelegate()

  context(Raise<WorkflowError>)
  final override suspend fun invoke(request: R): E {
    log.debug("Executing workflow ${this.className()} with request $request")
    return either {
      try {
        execute(request)
      } catch(ex: ValidationException) {
        log.error("Workflow Error on request: $request", ex)
        raise(WorkflowValidationError(ex.errors))
      }
    }
      .getOrElse {
        log.error("Workflow Error on request: $request", it)
        raise(it)
      }
  }

  context(Raise<WorkflowError>)
  abstract suspend fun execute(request: R): E
}



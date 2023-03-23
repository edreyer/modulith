package io.liquidsoftware.common.workflow

import arrow.core.continuations.EffectScope
import arrow.core.continuations.effect
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
  context(EffectScope<WorkflowError>)
  suspend fun invoke(request: R): E
}

abstract class BaseSafeWorkflow<R: Request, E : Event> : SafeWorkflow<R, E> {
  private val log by LoggerDelegate()

  context(EffectScope<WorkflowError>)
  final override suspend fun invoke(request: R): E {
    log.debug("Executing workflow ${this.className()} with request $request")
    return effect {
      try {
        execute(request)
      } catch(ex: ValidationException) {
        log.error("Workflow Error on request: $request", ex)
        shift(WorkflowValidationError(ex.errors))
      }
    }
      .handleError { err ->
        log.error("Workflow Error on request: $request", err)
        shift(err)
      }
      .bind()
  }

  context(EffectScope<WorkflowError>)
  abstract suspend fun execute(request: R): E
}



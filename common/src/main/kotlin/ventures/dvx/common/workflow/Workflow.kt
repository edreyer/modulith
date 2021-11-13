package ventures.dvx.common.workflow

import arrow.core.computations.result
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

// Workflow Input type
sealed interface Request
interface Command : Request
interface Query : Request

// Workflow Output type
interface Event

interface Workflow<E : Event>
interface SafeWorkflow<R: Request, E : Event> : Workflow<E> {
  suspend fun invoke(request: R): Result<E>
}
interface SecuredWorkflow<R: Request, E : Event> : Secured<R>

abstract class BaseSafeWorkflow<R: Request, E : Event> : SafeWorkflow<R, E> {
  override suspend operator fun invoke(request: R): Result<E> =
    result {
      execute(request)
    }
  abstract suspend fun execute(request: R): E
}

abstract class BaseSafeSecureWorkflow<R: Request, E : Event> : SafeWorkflow<R, E>, SecuredWorkflow<R, E> {
  override suspend fun invoke(request: R): Result<E> =
    result {
      assertCanPerform(request)
      execute(request)
    }
  abstract suspend fun execute(request: R): E
}

interface MonoWorkflow<R: Request, E : Event> : Workflow<E> {
  suspend operator fun invoke(request: R): Mono<E> {
    return execute(request)
  }
  suspend fun execute(request: R): Mono<E>
}

interface FluxWorkflow<R: Request, E : Event> : Workflow<E> {
  suspend operator fun invoke(request: R): Flux<E> {
    return execute(request)
  }
  suspend fun execute(request: R): Flux<E>
}



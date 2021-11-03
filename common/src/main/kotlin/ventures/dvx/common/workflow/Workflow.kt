package ventures.dvx.common.workflow

import arrow.core.computations.result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.withContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

// Workflow Input type
sealed interface Request
interface Command : Request
interface Query : Request

// Workflow Output type
interface Event

interface Workflow<E : Event> {
  // TODO: Bridgekeeper integration
//  val party: Party
//  val resourceType: ResourceType
//  val bridgeKeeper: BridgeKeeper
//
//  fun assertCanPerform() =
//    bridgeKeeper.assertCanPerform(party, resourceType, this.className())
}
interface UnsafeWorkflow<R: Request, E : Event> : Workflow<E> {
  suspend fun invoke(request: R): E
}
interface SafeWorkflow<R: Request, E : Event> : Workflow<E> {
  suspend operator fun invoke(request: R): Result<E>
}
interface MonoWorkflow<R: Request, E : Event> : Workflow<E> {
  suspend fun invoke(request: R): Mono<E>
}
interface FluxWorkflow<R: Request, E : Event> : Workflow<E> {
  suspend fun invoke(request: R): Flux<E>
}

//typealias UnsafeUseCase<I, R> = UnsafeWorkflow<I, R>
//typealias SafeUseCase<I, R> = SafeWorkflow<I, R>
//typealias MonoUseCase<I, R> = MonoWorkflow<I, R>
//typealias FluxUseCase<I, R> = FluxWorkflow<I, R>

suspend fun <R: Request, E : Event> runAsyncUnsafe(workflow: UnsafeWorkflow<R, E>, request: R): Result<E> = coroutineScope {
  withContext(Dispatchers.Default) {
    result {
      workflow.invoke(request)
    }
  }
}

suspend fun <R: Request, E : Event> runAsync(workflow: SafeWorkflow<R, E>, request: R): Result<E> = coroutineScope {
  withContext(Dispatchers.Default) {
    workflow.invoke(request)
  }
}

suspend fun <R: Request, E : Event> runAsyncMono(workflow: MonoWorkflow<R, E>, request: R): Result<E> = coroutineScope {
  withContext(Dispatchers.Default) {
    result {
      workflow.invoke(request).awaitSingle()
    }
  }
}

suspend fun <R: Request, E : Event> runAsyncFlux(workflow: FluxWorkflow<R, E>, request: R): Flux<E> = coroutineScope {
  withContext(Dispatchers.Default) {
    try {
      workflow.invoke(request)
    } catch (e: Throwable) { Flux.error(e) }
  }
}



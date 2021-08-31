package ventures.dvx.common.workflow

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

typealias Workflow<R> = suspend () -> Result<R>
typealias MonoWorkflow<R> = suspend () -> Mono<R>
typealias FluxWorkflow<R> = suspend () -> Flux<R>

typealias UseCase<R> = Workflow<R>
typealias MonoUseCase<R> = MonoWorkflow<R>
typealias FluxUseCase<R> = FluxWorkflow<R>

suspend fun <R : Any> runAsync(workflow: Workflow<R>): Result<R> = coroutineScope {
  async {
    try {
      workflow.invoke()
    } catch (e: Throwable) { Result.failure(e)}
  }.await()
}

suspend fun <R : Any> runAsyncMono(workflow: MonoWorkflow<R>): Mono<R> = coroutineScope {
  async {
    workflow.invoke()
  }.await()
}

suspend fun <R : Any> runAsyncFlux(workflow: FluxWorkflow<R>): Flux<R> = coroutineScope {
  async {
    try {
      workflow.invoke()
    } catch (e: Throwable) { Flux.error(e) }
  }.await()
}

interface WorkflowRunner<T, R> {
  suspend fun run(input: T): R

}

interface UseCaseRunner<T, R> {
  suspend fun run(input: T): R
}

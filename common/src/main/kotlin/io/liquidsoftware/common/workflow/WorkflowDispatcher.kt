package io.liquidsoftware.common.workflow

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import io.liquidsoftware.common.logging.LoggerDelegate
import io.liquidsoftware.common.security.SecurityCoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass

/*
 Inspired by: https://medium.com/swlh/cqrs-and-application-pipelines-in-kotlin-441d8f7fe427
 */
object WorkflowDispatcher {
  val log by LoggerDelegate()

  val queryHandlers = mutableMapOf<KClass<Query>, SafeWorkflow<Query, *>>()
  val commandHandlers = mutableMapOf<KClass<Command>, SafeWorkflow<Command, *>>()
//  Use this to support List of Events
//  val commandHandlers = mutableMapOf<KClass<Command>, MutableList<SafeWorkflow<Command, *>>>()

  inline fun <reified Q : Query, reified E : Event> registerQueryHandler(handler: SafeWorkflow<Q, E>) {
    @Suppress("UNCHECKED_CAST")
    queryHandlers[Q::class as KClass<Query>] = handler as SafeWorkflow<Query, E>
  }

  inline fun <reified C : Command, reified E : Event> registerCommandHandler(handler: SafeWorkflow<C, E>) {
    @Suppress("UNCHECKED_CAST")
    commandHandlers[C::class as KClass<Command>] = handler as SafeWorkflow<Command, E>
  }

// Use this to support List of Events
//  inline fun <reified C : Command, reified E : Event> registerCommandHandler(handler: SafeWorkflow<C, E>) {
//    @Suppress("UNCHECKED_CAST")
//    val commandClass = C::class as KClass<Command>
//    val commandList = commandHandlers.putIfAbsent(commandClass, mutableListOf()) ?: commandHandlers[commandClass]!!
//    @Suppress("UNCHECKED_CAST")
//    commandList.add(handler as SafeWorkflow<Command, *>)
//  }

  suspend fun <E: Event> dispatch(query: Query): Either<WorkflowError, E> =
    withContext(Dispatchers.Default + SecurityCoroutineContext()) {
      either {
        ensureNotNull(queryHandlers[query::class]) {
          MissingHandler("No handler for query $query")
        }.let {
          it.invoke(query) as E
        }
      }
    }

  // Use this to support List of Events
//  @Suppress("UNCHECKED_CAST")
//  suspend fun <E: Event> dispatch(command: Command): List<Result<E>> =
//    commandHandlers[command::class]?.map { runAsync(it as SafeWorkflow<Command, E>, command) } ?: emptyList()

  suspend fun <E: Event> dispatch(command: Command): Either<WorkflowError, E> =
    withContext(Dispatchers.Default + SecurityCoroutineContext()) {
      either {
        ensureNotNull(commandHandlers[command::class]) {
          MissingHandler("No handler for command $command")
        }.let {
          it.invoke(command) as E
        }
      }
    }

}

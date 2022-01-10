package io.liquidsoftware.common.workflow

import io.liquidsoftware.common.logging.LoggerDelegate
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
    queryHandlers[Q::class as KClass<Query>] = handler as SafeWorkflow<Query, *>
  }

  inline fun <reified C : Command, reified E : Event> registerCommandHandler(handler: SafeWorkflow<C, E>) {
    @Suppress("UNCHECKED_CAST")
    commandHandlers[C::class as KClass<Command>] = handler as SafeWorkflow<Command, *>
  }

// Use this to support List of Events
//  inline fun <reified C : Command, reified E : Event> registerCommandHandler(handler: SafeWorkflow<C, E>) {
//    @Suppress("UNCHECKED_CAST")
//    val commandClass = C::class as KClass<Command>
//    val commandList = commandHandlers.putIfAbsent(commandClass, mutableListOf()) ?: commandHandlers[commandClass]!!
//    @Suppress("UNCHECKED_CAST")
//    commandList.add(handler as SafeWorkflow<Command, *>)
//  }

  @Suppress("UNCHECKED_CAST")
  suspend fun <E: Event> dispatch(query: Query): Result<E> =
    queryHandlers[query::class]?.let {
      it.invoke(query) as Result<E>
    }?.onFailure {
      ex -> log.error("Query Error", ex)
    } ?: throw IllegalStateException("No handler for query")

  // Use this to support List of Events
//  @Suppress("UNCHECKED_CAST")
//  suspend fun <E: Event> dispatch(command: Command): List<Result<E>> =
//    commandHandlers[command::class]?.map { runAsync(it as SafeWorkflow<Command, E>, command) } ?: emptyList()

  @Suppress("UNCHECKED_CAST")
  suspend fun <E: Event> dispatch(command: Command): Result<E> =
    commandHandlers[command::class]?.let {
      it.invoke(command) as Result<E>
    }?.onFailure {
        ex -> log.error("Command Error", ex)
    } ?: throw IllegalStateException("No handler for query")

}

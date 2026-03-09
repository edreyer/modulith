package io.liquidsoftware.common.usecase

import io.liquidsoftware.workflow.BaseEvent
import io.liquidsoftware.workflow.UseCaseCommand
import io.liquidsoftware.workflow.WorkflowChainBuilderFactory
import java.time.Instant
import java.util.UUID

typealias Event = io.liquidsoftware.workflow.Event
typealias Key<T> = io.liquidsoftware.workflow.Key<T>
typealias ParallelErrorPolicy = io.liquidsoftware.workflow.ParallelErrorPolicy
typealias UseCase<C> = io.liquidsoftware.workflow.UseCase<C>
typealias UseCaseEvents = io.liquidsoftware.workflow.UseCaseEvents
typealias UseCaseResult<S> = io.liquidsoftware.workflow.UseCaseResult<S>
typealias Workflow<I, O> = io.liquidsoftware.workflow.Workflow<I, O>
typealias WorkflowContext = io.liquidsoftware.workflow.WorkflowContext
typealias WorkflowError = io.liquidsoftware.workflow.WorkflowError
typealias WorkflowExecution = io.liquidsoftware.workflow.WorkflowExecution
typealias WorkflowResult<S> = io.liquidsoftware.workflow.WorkflowResult<S>
typealias WorkflowState = io.liquidsoftware.workflow.WorkflowState

interface Command : UseCaseCommand

interface Query : UseCaseCommand

abstract class AppEvent(
  id: UUID = UUID.randomUUID(),
  timestamp: Instant = Instant.now(),
) : BaseEvent(id, timestamp)

fun <C : UseCaseCommand> useCase(
  block: WorkflowChainBuilderFactory<C>.() -> Unit
): UseCase<C> = io.liquidsoftware.workflow.useCase(block)

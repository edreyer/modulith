package io.liquidsoftware.common.usecase.legacy

import arrow.core.Either
import io.liquidsoftware.common.workflow.WorkflowError as LegacyWorkflowError
import io.liquidsoftware.workflow.Event
import io.liquidsoftware.workflow.UseCase
import io.liquidsoftware.workflow.UseCaseCommand
import io.liquidsoftware.workflow.UseCaseResult
import io.liquidsoftware.workflow.WorkflowError as UseCaseError
import io.liquidsoftware.workflow.WorkflowState

suspend fun <LegacyRequest, C : UseCaseCommand, O> UseCase<C>.executeLegacyProjected(
  request: LegacyRequest,
  requestMapper: (LegacyRequest) -> C,
  projector: (UseCaseResult<WorkflowState>) -> Either<UseCaseError, O>,
  domainErrorMapper: (UseCaseError.DomainError) -> LegacyWorkflowError? = { null },
): Either<LegacyWorkflowError, O> =
  executeProjected(requestMapper(request), projector = projector).toLegacyEither(domainErrorMapper)

suspend inline fun <LegacyRequest, C : UseCaseCommand, reified E : Event> UseCase<C>.executeLegacyForEvent(
  request: LegacyRequest,
  noinline requestMapper: (LegacyRequest) -> C,
  noinline domainErrorMapper: (UseCaseError.DomainError) -> LegacyWorkflowError? = { null },
): Either<LegacyWorkflowError, E> =
  executeForEvent<E>(requestMapper(request)).toLegacyEither(domainErrorMapper)

suspend inline fun <LegacyRequest, C : UseCaseCommand, reified S : WorkflowState> UseCase<C>.executeLegacyForState(
  request: LegacyRequest,
  noinline requestMapper: (LegacyRequest) -> C,
  noinline domainErrorMapper: (UseCaseError.DomainError) -> LegacyWorkflowError? = { null },
): Either<LegacyWorkflowError, S> =
  executeForState<S>(requestMapper(request)).toLegacyEither(domainErrorMapper)

package io.liquidsoftware.common.usecase

import arrow.core.Either
import arrow.core.nonEmptyListOf
import io.liquidsoftware.common.types.ValidationError
import io.liquidsoftware.common.workflow.MissingHandler
import io.liquidsoftware.common.workflow.ServerError
import io.liquidsoftware.common.workflow.UnauthorizedWorkflowError
import io.liquidsoftware.common.workflow.WorkflowValidationError
import io.liquidsoftware.common.workflow.WorkflowError as LegacyWorkflowError
import io.liquidsoftware.workflow.WorkflowError as UseCaseError

const val UNAUTHORIZED_DOMAIN_CODE = "UNAUTHORIZED"

fun LegacyWorkflowError.toUseCaseError(
  domainMapper: (LegacyWorkflowError) -> UseCaseError.DomainError? = { null },
): UseCaseError =
  domainMapper(this) ?: when (this) {
    is UnauthorizedWorkflowError -> UseCaseError.DomainError(UNAUTHORIZED_DOMAIN_CODE, message)
    is WorkflowValidationError -> UseCaseError.ValidationError(message)
    is ServerError -> UseCaseError.ExecutionError(msg)
    is MissingHandler -> UseCaseError.ExecutionError(message)
    else -> UseCaseError.ExecutionError(message)
  }

fun UseCaseError.toWorkflowError(
  domainMapper: (UseCaseError.DomainError) -> LegacyWorkflowError? = { null },
): LegacyWorkflowError = when (this) {
  is UseCaseError.DomainError -> domainMapper(this) ?: when (code) {
    UNAUTHORIZED_DOMAIN_CODE -> UnauthorizedWorkflowError(message)
    else -> ServerError(message)
  }
  is UseCaseError.ValidationError -> WorkflowValidationError(nonEmptyListOf(ValidationError(message)))
  is UseCaseError.ExecutionError -> ServerError(message)
  is UseCaseError.ExceptionError -> ServerError("$message: ${ex.message ?: ex::class.simpleName}")
  is UseCaseError.CompositionError -> ServerError(message)
  is UseCaseError.ExecutionContextError -> error.toWorkflowError(domainMapper)
  is UseCaseError.ChainError -> error.toWorkflowError(domainMapper)
}

fun <T> Either<LegacyWorkflowError, T>.toUseCaseEither(
  errorMapper: (LegacyWorkflowError) -> UseCaseError.DomainError? = { null },
): Either<UseCaseError, T> = mapLeft { legacyError ->
  legacyError.toUseCaseError(errorMapper)
}

fun <T> Either<UseCaseError, T>.toWorkflowEither(
  domainMapper: (UseCaseError.DomainError) -> LegacyWorkflowError? = { null },
): Either<LegacyWorkflowError, T> = fold(
  { Either.Left(it.toWorkflowError(domainMapper)) },
  { Either.Right(it) },
)

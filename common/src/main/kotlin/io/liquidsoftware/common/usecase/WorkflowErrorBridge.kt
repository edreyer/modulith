package io.liquidsoftware.common.usecase

import arrow.core.Either
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

fun <T> Either<LegacyWorkflowError, T>.toUseCaseEither(
  errorMapper: (LegacyWorkflowError) -> UseCaseError.DomainError? = { null },
): Either<UseCaseError, T> = mapLeft { legacyError ->
  legacyError.toUseCaseError(errorMapper)
}

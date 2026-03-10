package io.liquidsoftware.common.application.error

import arrow.core.Either
import io.liquidsoftware.common.usecase.UNAUTHORIZED_DOMAIN_CODE
import io.liquidsoftware.workflow.WorkflowError as UseCaseError

/**
 * Maps the shared application-contract error model into OSS workflow-engine errors.
 *
 * This is useful when a use case consumes another module's `*-api` contract and needs to
 * continue working inside the OSS workflow DSL.
 */
fun ApplicationError.toUseCaseError(
  domainMapper: (ApplicationError) -> UseCaseError.DomainError? = { null },
): UseCaseError =
  domainMapper(this) ?: when (this) {
    is ApplicationError.Unauthorized -> UseCaseError.DomainError(UNAUTHORIZED_DOMAIN_CODE, message)
    is ApplicationError.Validation -> UseCaseError.ValidationError(message)
    is ApplicationError.Unexpected -> UseCaseError.ExecutionError(message)
    else -> UseCaseError.DomainError(code, message)
  }

/**
 * Convenience bridge for `Either<ApplicationError, T>` results consumed inside OSS workflow use cases.
 */
fun <T> Either<ApplicationError, T>.toUseCaseApplicationEither(
  errorMapper: (ApplicationError) -> UseCaseError.DomainError? = { null },
): Either<UseCaseError, T> = mapLeft { applicationError ->
  applicationError.toUseCaseError(errorMapper)
}

/**
 * Maps OSS workflow-engine errors into the shared application-contract error model.
 *
 * [domainMapper] lets callers preserve explicit domain semantics carried in `DomainError` values.
 */
fun UseCaseError.toApplicationError(
  domainMapper: (UseCaseError.DomainError) -> ApplicationError? = { null },
): ApplicationError = when (this) {
  is UseCaseError.DomainError -> domainMapper(this) ?: when (code) {
    UNAUTHORIZED_DOMAIN_CODE -> ApplicationError.Unauthorized(message = message)
    else -> ApplicationError.Unexpected(code = code, message = message)
  }
  is UseCaseError.ValidationError -> ApplicationError.Validation(message = message)
  is UseCaseError.ExecutionError -> ApplicationError.Unexpected(message = message)
  is UseCaseError.ExceptionError ->
    ApplicationError.Unexpected(message = "$message: ${ex.message ?: ex::class.simpleName}")
  is UseCaseError.CompositionError -> ApplicationError.Unexpected(message = message)
  is UseCaseError.ExecutionContextError -> error.toApplicationError(domainMapper)
  is UseCaseError.ChainError -> error.toApplicationError(domainMapper)
}

/**
 * Convenience bridge for `Either<UseCaseError, T>` results from OSS workflow use cases.
 */
fun <T> Either<UseCaseError, T>.toApplicationUseCaseEither(
  errorMapper: (UseCaseError.DomainError) -> ApplicationError? = { null },
): Either<ApplicationError, T> = mapLeft { useCaseError ->
  useCaseError.toApplicationError(errorMapper)
}

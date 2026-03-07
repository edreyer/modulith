package io.liquidsoftware.common.ext

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.context.bind
import io.liquidsoftware.common.types.ValidationErrors
import io.liquidsoftware.common.workflow.WorkflowError
import io.liquidsoftware.common.workflow.WorkflowValidationError

context(_: Raise<WorkflowError>)
inline fun <A> Either<ValidationErrors, A>.bindValidation(
  crossinline mapError: (ValidationErrors) -> WorkflowError = ::WorkflowValidationError
): A = mapLeft { mapError(it) }.bind()

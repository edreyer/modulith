package io.liquidsoftware.common.workflow

import arrow.core.Nel
import io.liquidsoftware.common.types.ValidationError
import io.liquidsoftware.common.types.toErrString
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

data class MissingHandler(override val message: String) : WorkflowError(message)

@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
data class ServerError(val msg: String) : WorkflowError(msg) {
  override val message = "Server Error: $msg"
}

@ResponseStatus(code = HttpStatus.PRECONDITION_FAILED)
data class ValidationErrors(private val errors: Nel<ValidationError>): WorkflowError(errors.toErrString()) {
  override val message = errors.toErrString()
}

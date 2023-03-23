package io.liquidsoftware.common.workflow

import io.liquidsoftware.common.types.ValidationErrors
import io.liquidsoftware.common.types.toErrString
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

data class MissingHandler(override val message: String) : WorkflowError(message)

@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
data class ServerError(val msg: String) : WorkflowError(msg) {
  override val message = "Server Error: $msg"
}

@ResponseStatus(code = HttpStatus.PRECONDITION_FAILED)
data class WorkflowValidationError(private val errors: ValidationErrors): WorkflowError(errors.toErrString()) {
  override val message = errors.toErrString()
}

package io.liquidsoftware.common.web

import arrow.core.Either
import io.liquidsoftware.common.ext.hasResponseStatus
import io.liquidsoftware.common.workflow.WorkflowError

interface ControllerSupport {

  fun <T> Either<WorkflowError, T>.throwIfSpringError(): Either<WorkflowError, T> {
    onLeft { if (it.hasResponseStatus()) throw it }
    return this
  }

}

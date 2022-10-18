package io.liquidsoftware.common.errors

import arrow.core.Nel
import io.liquidsoftware.common.logging.LoggerDelegate
import io.liquidsoftware.common.types.ValidationError
import io.liquidsoftware.common.types.ValidationException
import io.liquidsoftware.common.types.toErrString

object ErrorHandling {
  val logger by LoggerDelegate()

  val ERROR_HANDLER = { errors: Nel<ValidationError> ->
    val err = errors.toErrString()
    logger.error(err)
    throw ValidationException(errors)
  }

}


package io.liquidsoftware.common.errors

import io.liquidsoftware.common.logging.LoggerDelegate
import io.liquidsoftware.common.types.ValidationErrors
import io.liquidsoftware.common.types.ValidationException
import io.liquidsoftware.common.types.toErrString

object ErrorHandling {
  val logger by LoggerDelegate()

  val ERROR_HANDLER = { errors: ValidationErrors ->
    val err = errors.toErrString()
    logger.error(err)
    throw ValidationException(errors)
  }

}


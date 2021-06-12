package ventures.dvx.common.error

import me.alidg.errors.annotation.ExceptionMapping
import me.alidg.errors.annotation.ExposeAsArg
import org.springframework.http.HttpStatus

@ExceptionMapping(statusCode = HttpStatus.PRECONDITION_FAILED, errorCode = "error.http.412")
data class PreconditionFailedException(
  @ExposeAsArg(0) val msg: String
): RuntimeException(msg)

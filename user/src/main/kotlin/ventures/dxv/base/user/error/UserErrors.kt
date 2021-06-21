package ventures.dxv.base.user.error

import org.axonframework.commandhandling.CommandExecutionException
import org.axonframework.queryhandling.QueryExecutionException
import ventures.dvx.base.user.api.UserError


class UserException : RuntimeException {
  override val message: String // makes message non-nullable
  val userError: UserError
  constructor(userError: UserError) : super(userError.msg) {
    this.message = userError.msg
    this.userError = userError
  }
  constructor(userError: UserError, throwable: Throwable) : super(userError.msg, throwable) {
    this.message = userError.msg
    this.userError = userError
  }
}

data class UserCommandException(
  override val message: String,
  val details: UserError,
  override val cause: Throwable?,
): CommandExecutionException(message, cause, details) {
  constructor(msg: String, details: UserError): this(msg, details, null)
}

data class UserQueryException(
  override val message: String,
  val details: UserError,
  override val cause: Throwable?,
): QueryExecutionException(message, cause, details) {
  constructor(msg: String, details: UserError): this(msg, details, null)
}


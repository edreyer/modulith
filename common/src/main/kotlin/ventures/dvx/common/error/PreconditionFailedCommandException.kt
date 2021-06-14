package ventures.dvx.common.error

import org.axonframework.commandhandling.CommandExecutionException

data class PreconditionFailedCommandException(
  val msg: String,
  override val cause: Throwable?,
  val details: Any?
): CommandExecutionException(msg, cause, details) {
  constructor(msg: String): this(msg, null, msg)
}

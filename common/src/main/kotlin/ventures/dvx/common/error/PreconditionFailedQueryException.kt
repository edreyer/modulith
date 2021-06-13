package ventures.dvx.common.error

import org.axonframework.queryhandling.QueryExecutionException

data class PreconditionFailedQueryException(
  val msg: String,
  override val cause: Throwable?,
  val details: Any?
): QueryExecutionException(msg, cause, details)

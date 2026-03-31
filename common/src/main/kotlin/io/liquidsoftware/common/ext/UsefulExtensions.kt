package io.liquidsoftware.common.ext

import arrow.core.raise.Raise
import arrow.core.raise.context.raise
import com.mongodb.MongoException
import io.liquidsoftware.common.security.acl.AccessDenied
import io.liquidsoftware.common.security.acl.AuthorizationError
import io.liquidsoftware.common.security.acl.DenialContext
import io.liquidsoftware.common.workflow.ServerError
import io.liquidsoftware.common.workflow.UnauthorizedWorkflowError
import io.liquidsoftware.common.workflow.WorkflowError
import kotlinx.coroutines.CancellationException
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.dao.DataAccessException

/* Get the name of any class */
fun Any.className(): String = this::class.qualifiedName ?: this::class.java.name

fun Throwable.hasResponseStatus(): Boolean = this.javaClass.isAnnotationPresent(ResponseStatus::class.java)

fun Throwable.toWorkflowError(): WorkflowError = when (this) {
  is WorkflowError -> this
  else -> ServerError(message ?: "Unexpected error")
}

fun AuthorizationError.toWorkflowError(): WorkflowError = when (this) {
  is AccessDenied -> UnauthorizedWorkflowError(
    when (val denialContext = context) {
      is DenialContext.Acl ->
        "No access to: ${denialContext.resourceId} Permission: $permission Subject: ${denialContext.subjectId}"
      DenialContext.Unknown ->
        "No access. Permission: $permission"
    }
  )
}

context(_: Raise<WorkflowError>)
suspend fun <T> workflowBoundary(block: suspend () -> T): T = try {
  block()
} catch (ex: CancellationException) {
  throw ex
} catch (ex: DataAccessException) {
  raise(ex.toWorkflowError())
} catch (ex: MongoException) {
  raise(ex.toWorkflowError())
}

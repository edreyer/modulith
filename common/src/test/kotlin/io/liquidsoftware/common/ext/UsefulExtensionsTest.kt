package io.liquidsoftware.common.ext

import arrow.core.Either
import arrow.core.raise.either
import com.mongodb.MongoException
import io.liquidsoftware.common.security.acl.Permission
import io.liquidsoftware.common.security.acl.PermissionDenied
import io.liquidsoftware.common.workflow.ServerError
import io.liquidsoftware.common.workflow.UnauthorizedWorkflowError
import io.liquidsoftware.common.workflow.WorkflowError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.dao.DataAccessResourceFailureException

class UsefulExtensionsTest {

  @Test
  fun `permission denied maps to UnauthorizedWorkflowError`() {
    val error = PermissionDenied(
      resourceId = "appointment-1",
      permission = Permission.WRITE,
      subjectId = "user-1",
    ).toWorkflowError()

    assertInstanceOf(UnauthorizedWorkflowError::class.java, error)
    assertEquals(
      "No access to: appointment-1 Permission: WRITE Subject: user-1",
      error.message
    )
  }

  @Test
  fun `workflowBoundary maps data access exceptions to ServerError`() = runBlocking {
    val result = either<WorkflowError, Unit> {
      workflowBoundary { throw DataAccessResourceFailureException("db down") }
    }

    val error = (result as Either.Left).value
    assertInstanceOf(ServerError::class.java, error)
    assertEquals("Server Error: db down", error.message)
  }

  @Test
  fun `workflowBoundary maps mongo exceptions to ServerError`() = runBlocking {
    val result = either<WorkflowError, Unit> {
      workflowBoundary { throw MongoException(1, "mongo down") }
    }

    val error = (result as Either.Left).value
    assertInstanceOf(ServerError::class.java, error)
    assertEquals("Server Error: mongo down", error.message)
  }

  @Test
  fun `workflowBoundary rethrows cancellation exceptions`() {
    assertThrows(CancellationException::class.java) {
      runBlocking {
        either<WorkflowError, Unit> {
          workflowBoundary { throw CancellationException("cancelled") }
        }
      }
    }
  }
}

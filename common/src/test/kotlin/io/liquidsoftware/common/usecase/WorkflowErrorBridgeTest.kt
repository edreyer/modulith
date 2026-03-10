package io.liquidsoftware.common.usecase

import arrow.core.Either
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import org.junit.jupiter.api.Test
import io.liquidsoftware.common.workflow.WorkflowError as LegacyWorkflowError
import io.liquidsoftware.workflow.WorkflowError as UseCaseError

class WorkflowErrorBridgeTest {

  @Test
  fun `workflow error maps to use case execution error by default`() {
    val workflowError = object : LegacyWorkflowError("boom") {}

    val mapped = Either.Left(workflowError)
      .toUseCaseEither()
      .fold({ it }, { error("expected error") })

    assertThat(mapped).isInstanceOf(UseCaseError.ExecutionError::class.java)
    assertThat((mapped as UseCaseError.ExecutionError).message).isEqualTo("boom")
  }

  @Test
  fun `domain mapper is used when mapping workflow error to use case error`() {
    val workflowError = object : LegacyWorkflowError("missing") {}

    val mapped = workflowError.toUseCaseError { domainError ->
      UseCaseError.DomainError("USER_NOT_FOUND", "mapped:${domainError.message}")
    }

    assertThat(mapped).isInstanceOf(UseCaseError.DomainError::class.java)
    assertThat((mapped as UseCaseError.DomainError).message).isEqualTo("mapped:missing")
  }
}

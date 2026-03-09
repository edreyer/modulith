package io.liquidsoftware.common.usecase

import arrow.core.Either
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import io.liquidsoftware.common.workflow.WorkflowError as LegacyWorkflowError
import io.liquidsoftware.workflow.WorkflowError as UseCaseError

class WorkflowErrorBridgeTest {

  @Test
  fun `workflow error round trips through use case mapping`() {
    val workflowError = object : LegacyWorkflowError("boom") {}

    val mapped = Either.Left(workflowError)
      .toUseCaseEither()
      .fold({ it }, { error("expected error") })

    val roundTrip = mapped.toWorkflowError()

    assertThat(roundTrip.message).isEqualTo("Server Error: boom")
  }

  @Test
  fun `domain mapper is used for round trip`() {
    val useCaseError = UseCaseError.DomainError("USER_NOT_FOUND", "missing")

    val roundTrip = useCaseError.toWorkflowError { domainError ->
      when (domainError.code) {
        "USER_NOT_FOUND" -> object : LegacyWorkflowError("mapped:${domainError.message}") {}
        "UNUSED" -> object : LegacyWorkflowError("unused") {}
        else -> null
      }
    }

    assertThat(roundTrip.message).isEqualTo("mapped:missing")
  }
}

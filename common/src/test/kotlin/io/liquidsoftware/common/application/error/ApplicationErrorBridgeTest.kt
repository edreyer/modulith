package io.liquidsoftware.common.application.error

import arrow.core.Either
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import io.liquidsoftware.common.usecase.UNAUTHORIZED_DOMAIN_CODE
import io.liquidsoftware.workflow.WorkflowError as UseCaseError
import org.junit.jupiter.api.Test

class ApplicationErrorBridgeTest {

  @Test
  fun `use case unauthorized domain error maps to application unauthorized`() {
    val result = Either.Left(UseCaseError.DomainError(UNAUTHORIZED_DOMAIN_CODE, "nope"))
      .toApplicationUseCaseEither()
      .fold({ it }, { error("expected error") })

    assertThat(result).isInstanceOf(ApplicationError.Unauthorized::class.java)
    assertThat(result.code).isEqualTo("UNAUTHORIZED")
    assertThat(result.message).isEqualTo("nope")
  }

  @Test
  fun `domain mapper is used when mapping use case error to application error`() {
    val result = UseCaseError.DomainError("USER_NOT_FOUND", "missing").toApplicationError {
      ApplicationError.NotFound(code = it.code, message = it.message)
    }

    assertThat(result).isInstanceOf(ApplicationError.NotFound::class.java)
    assertThat(result.code).isEqualTo("USER_NOT_FOUND")
  }

  @Test
  fun `application validation maps to use case validation error`() {
    val error = ApplicationError.Validation(message = "bad input")

    val mapped = error.toUseCaseError()

    assertThat(mapped).isInstanceOf(UseCaseError.ValidationError::class.java)
    assertThat((mapped as UseCaseError.ValidationError).message.trim()).isEqualTo("bad input")
  }

  @Test
  fun `application error custom mapper is used for use case mapping`() {
    val error = ApplicationError.NotFound(code = "USER_NOT_FOUND", message = "missing")

    val mapped = error.toUseCaseError { applicationError ->
      when (applicationError.code) {
        "USER_NOT_FOUND" -> UseCaseError.DomainError(applicationError.code, "mapped:${applicationError.message}")
        else -> null
      }
    }

    assertThat(mapped).isInstanceOf(UseCaseError.DomainError::class.java)
    assertThat((mapped as UseCaseError.DomainError).message).isEqualTo("mapped:missing")
  }
}

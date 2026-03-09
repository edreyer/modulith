package io.liquidsoftware.common.usecase.legacy

import arrow.core.Either
import arrow.core.nonEmptyListOf
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import io.liquidsoftware.common.types.ValidationError
import io.liquidsoftware.common.usecase.Command
import io.liquidsoftware.common.usecase.WorkflowState
import io.liquidsoftware.common.usecase.useCase
import io.liquidsoftware.common.workflow.MissingHandler
import io.liquidsoftware.common.workflow.ServerError
import io.liquidsoftware.common.workflow.UnauthorizedWorkflowError
import io.liquidsoftware.common.workflow.WorkflowValidationError
import io.liquidsoftware.workflow.WorkflowError
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class LegacyWorkflowBridgeTest {

  @Test
  fun `unauthorized legacy errors map to domain errors and back`() {
    val useCaseError = UnauthorizedWorkflowError("No access").toUseCaseError()
    val roundTrip = useCaseError.toLegacyWorkflowError()

    assertThat(useCaseError).isInstanceOf(WorkflowError.DomainError::class)
    val domainError = useCaseError as WorkflowError.DomainError
    assertThat(domainError.code).isEqualTo(UNAUTHORIZED_DOMAIN_CODE)
    assertThat(roundTrip).isInstanceOf(UnauthorizedWorkflowError::class)
    assertThat(roundTrip.message).isEqualTo("No access")
  }

  @Test
  fun `validation legacy errors map to validation errors and back`() {
    val legacyError = WorkflowValidationError(nonEmptyListOf(ValidationError("bad input")))
    val useCaseError = legacyError.toUseCaseError()
    val roundTrip = useCaseError.toLegacyWorkflowError()

    assertThat(useCaseError).isInstanceOf(WorkflowError.ValidationError::class)
    assertThat((useCaseError as WorkflowError.ValidationError).message.trim()).isEqualTo("bad input")
    assertThat(roundTrip).isInstanceOf(WorkflowValidationError::class)
    assertThat(roundTrip.message.trim()).isEqualTo("bad input")
  }

  @Test
  fun `execute legacy projected adapts request and maps domain errors`() = runBlocking {
    val migratedUseCase = useCase<NewQuery> {
      startWith { query -> Either.Right(LookupState(query.email)) }
    }

    val result = migratedUseCase.executeLegacyProjected(
      request = LegacyQuery("missing@liquidsoftware.io"),
      requestMapper = { NewQuery(it.email) },
      projector = {
        Either.Left(WorkflowError.DomainError("USER_NOT_FOUND", (it.state as LookupState).email))
      },
      domainErrorMapper = { domainError ->
        when (domainError.code) {
          "USER_NOT_FOUND" -> MissingHandler(domainError.message)
          else -> null
        }
      },
    )

    val error = result.fold({ it }, { error("expected legacy error") })
    assertThat(error).isInstanceOf(MissingHandler::class)
    assertThat(error.message).isEqualTo("missing@liquidsoftware.io")
  }

  @Test
  fun `execute legacy for state maps success through request adapter`() = runBlocking {
    val migratedUseCase = useCase<NewQuery> {
      startWith { query -> Either.Right(LookupState(query.email)) }
    }

    val result = migratedUseCase.executeLegacyForState<LegacyQuery, NewQuery, LookupState>(
      request = LegacyQuery("user@liquidsoftware.io"),
      requestMapper = { NewQuery(it.email) },
    )

    val state = result.fold({ error("unexpected legacy error: $it") }, { it })
    assertThat(state.email).isEqualTo("user@liquidsoftware.io")
  }

  private data class LegacyQuery(
    val email: String,
  )

  private data class NewQuery(
    val email: String,
  ) : Command

  private data class LookupState(
    val email: String,
  ) : WorkflowState
}

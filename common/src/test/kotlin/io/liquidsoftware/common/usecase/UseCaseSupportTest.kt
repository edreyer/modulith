package io.liquidsoftware.common.usecase

import arrow.core.Either
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class UseCaseSupportTest {

  @Test
  fun `app event supplies id and timestamp by default`() {
    val event = TestEvent("created")

    assertThat(event.id).isNotNull()
    assertThat(event.timestamp).isNotNull()
    assertThat(event.label).isEqualTo("created")
  }

  @Test
  fun `app event accepts explicit metadata`() {
    val expectedId = UUID.randomUUID()
    val expectedTimestamp = Instant.parse("2026-03-09T12:00:00Z")

    val event = TestEvent(
      label = "custom",
      providedId = expectedId,
      providedTimestamp = expectedTimestamp,
    )

    assertThat(event.id).isEqualTo(expectedId)
    assertThat(event.timestamp).isEqualTo(expectedTimestamp)
  }

  @Test
  fun `command and query remain workflow use case commands`() {
    val command: Any = TestCommand("value")
    val query: Any = TestQuery("value")

    assertThat(command is io.liquidsoftware.workflow.UseCaseCommand).isEqualTo(true)
    assertThat(query is io.liquidsoftware.workflow.UseCaseCommand).isEqualTo(true)
  }

  @Test
  fun `usecase wrapper executes with common command and app event`() = runBlocking {
    val workflow = TestWorkflow()
    val correlationId = "corr-123"

    val useCase = useCase<TestCommand> {
      startWith { Either.Right(StartedState(it.value)) }
      then(workflow)
    }

    val result = useCase.executeDetailed(
      TestCommand("hello"),
      WorkflowContext().addData(WorkflowContext.CORRELATION_ID, correlationId),
    ).fold({ error("unexpected workflow error: $it") }, { it })

    assertThat(result.events.filterIsInstance<TestEvent>().lastOrNull()?.label).isEqualTo("hello")
    assertThat(result.state).isEqualTo(FinishedState("hello"))
    assertThat(result.context.get(WorkflowContext.CORRELATION_ID)).isEqualTo(correlationId)
    assertThat(result.context.executions.size).isEqualTo(1)
  }

  private data class TestCommand(val value: String) : Command

  private data class TestQuery(val value: String) : Query

  private data class StartedState(val value: String) : WorkflowState

  private data class FinishedState(val value: String) : WorkflowState

  private data class TestEvent(
    val label: String,
    val providedId: UUID = UUID.randomUUID(),
    val providedTimestamp: Instant = Instant.now(),
  ) : AppEvent(providedId, providedTimestamp)

  private class TestWorkflow : Workflow<StartedState, FinishedState>() {
    override val id: String = "test-workflow"

    override suspend fun executeWorkflow(
      input: StartedState,
      context: WorkflowContext,
    ): Either<WorkflowError, WorkflowResult<FinishedState>> =
      Either.Right(
        WorkflowResult(
          state = FinishedState(input.value),
          events = listOf(TestEvent(input.value)),
          context = context,
        )
      )
  }
}

package io.liquidsoftware.base.booking.application.workflows

import arrow.core.Either
import io.liquidsoftware.base.booking.application.mapper.toDto
import io.liquidsoftware.base.booking.application.port.`in`.FetchUserAppointmentsQuery
import io.liquidsoftware.base.booking.application.port.`in`.UserAppointmentsFetchedEvent
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.domain.Appointment
import io.liquidsoftware.base.booking.domain.CancelledAppointment
import io.liquidsoftware.common.usecase.Command as UseCaseCommand
import io.liquidsoftware.common.usecase.Workflow as UseCaseWorkflow
import io.liquidsoftware.common.usecase.WorkflowContext
import io.liquidsoftware.common.usecase.WorkflowResult
import io.liquidsoftware.common.usecase.WorkflowState
import io.liquidsoftware.common.usecase.toUseCaseEither
import io.liquidsoftware.common.usecase.toWorkflowEither
import io.liquidsoftware.common.usecase.useCase
import io.liquidsoftware.common.workflow.WorkflowError as LegacyWorkflowError
import io.liquidsoftware.workflow.WorkflowError as UseCaseError
import org.springframework.data.domain.PageRequest

internal class FetchUserAppointmentsUseCase(
  private val findAppointmentPort: FindAppointmentPort,
) {

  private val useCase = useCase<FetchUserAppointmentsRequest> {
    startWith { request -> Either.Right(FetchUserAppointmentsState(request.userId, request.page, request.size)) }
    then(LoadUserAppointmentsStep("load-user-appointments", findAppointmentPort))
    then(BuildFetchedAppointmentsStep("emit-user-appointments-fetched"))
  }

  suspend fun execute(query: FetchUserAppointmentsQuery): Either<LegacyWorkflowError, UserAppointmentsFetchedEvent> =
    useCase.executeProjected(
      FetchUserAppointmentsRequest(query.userId, query.page, query.size),
      projector = { result ->
        result.requireState<FetchedAppointmentsState>("emit-user-appointments-fetched").fold(
          { Either.Left(it) },
          { state -> Either.Right(state.event) },
        )
      },
    ).toWorkflowEither()

  private class LoadUserAppointmentsStep(
    override val id: String,
    private val findAppointmentPort: FindAppointmentPort,
  ) : UseCaseWorkflow<FetchUserAppointmentsState, LoadedUserAppointmentsState>() {

    override suspend fun executeWorkflow(
      input: FetchUserAppointmentsState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<LoadedUserAppointmentsState>> =
      findAppointmentPort.findByUserId(input.userId, PageRequest.of(input.page, input.size))
        .toUseCaseEither()
        .map { appointments -> WorkflowResult(state = LoadedUserAppointmentsState(appointments), context = context) }
  }

  private class BuildFetchedAppointmentsStep(
    override val id: String,
  ) : UseCaseWorkflow<LoadedUserAppointmentsState, FetchedAppointmentsState>() {

    override suspend fun executeWorkflow(
      input: LoadedUserAppointmentsState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<FetchedAppointmentsState>> =
      Either.Right(
        WorkflowResult(
          state = FetchedAppointmentsState(
            UserAppointmentsFetchedEvent(
              input.appointments
                .filter { it !is CancelledAppointment }
                .map(Appointment::toDto)
            )
          ),
          context = context,
        )
      )
  }
}

private data class FetchUserAppointmentsRequest(
  val userId: String,
  val page: Int,
  val size: Int,
) : UseCaseCommand

private data class FetchUserAppointmentsState(
  val userId: String,
  val page: Int,
  val size: Int,
) : WorkflowState

private data class LoadedUserAppointmentsState(
  val appointments: List<Appointment>,
) : WorkflowState

private data class FetchedAppointmentsState(
  val event: UserAppointmentsFetchedEvent,
) : WorkflowState

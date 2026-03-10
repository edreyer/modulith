package io.liquidsoftware.base.booking.application.workflows

import arrow.core.Either
import arrow.core.flatMap
import io.liquidsoftware.base.booking.application.mapper.toDto
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentCompletedEvent
import io.liquidsoftware.base.booking.application.port.`in`.CompleteAppointmentCommand
import io.liquidsoftware.base.booking.application.port.out.AppointmentEventPort
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.domain.CompleteAppointment
import io.liquidsoftware.base.booking.domain.InProgressAppointment
import io.liquidsoftware.common.application.error.ApplicationError
import io.liquidsoftware.common.application.error.toApplicationUseCaseEither
import io.liquidsoftware.common.usecase.Command as UseCaseCommand
import io.liquidsoftware.common.usecase.Workflow as UseCaseWorkflow
import io.liquidsoftware.common.usecase.WorkflowContext
import io.liquidsoftware.common.usecase.WorkflowResult
import io.liquidsoftware.common.usecase.WorkflowState
import io.liquidsoftware.common.usecase.toUseCaseEither
import io.liquidsoftware.common.usecase.useCase
import io.liquidsoftware.workflow.WorkflowError as UseCaseError

internal class CompleteAppointmentUseCase(
  private val findAppointmentPort: FindAppointmentPort,
  private val appointmentEventPort: AppointmentEventPort,
) {

  private val useCase = useCase<CompleteAppointmentRequest> {
    startWith { request ->
      Either.Right(CompleteAppointmentState(request.appointmentId, request.notes))
    }
    then(LoadInProgressAppointmentStep("load-in-progress-appointment", findAppointmentPort))
    then(BuildCompletedAppointmentStep("build-completed-appointment"))
    then(PersistAppointmentCompletedStep("persist-appointment-completed", appointmentEventPort))
  }

  suspend fun execute(command: CompleteAppointmentCommand): Either<ApplicationError, AppointmentCompletedEvent> =
    useCase.executeProjected(
      CompleteAppointmentRequest(command.appointmentId, command.notes),
      projector = { result ->
        result.requireState<AppointmentCompletedPersistedState>("persist-appointment-completed").fold(
          { Either.Left(it) },
          { state -> Either.Right(state.event) },
        )
      },
    ).toApplicationUseCaseEither(::mapBookingDomainError)

  private class LoadInProgressAppointmentStep(
    override val id: String,
    private val findAppointmentPort: FindAppointmentPort,
  ) : UseCaseWorkflow<CompleteAppointmentState, LoadedInProgressAppointmentState>() {

    override suspend fun executeWorkflow(
      input: CompleteAppointmentState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<LoadedInProgressAppointmentState>> =
      findAppointmentPort.findStartedById(input.appointmentId)
        .toUseCaseEither()
        .flatMap { appointment ->
          appointment
            ?.let {
              Either.Right(
                WorkflowResult(
                  state = LoadedInProgressAppointmentState(appointment = it, notes = input.notes),
                  context = context,
                )
              )
            }
            ?: Either.Left(
              UseCaseError.DomainError(
                APPOINTMENT_NOT_FOUND_CODE,
                "Appointment Not Found. id=${input.appointmentId}, status=IN_PROGRESS",
              )
            )
        }
  }

  private class BuildCompletedAppointmentStep(
    override val id: String,
  ) : UseCaseWorkflow<LoadedInProgressAppointmentState, CompletedAppointmentState>() {

    override suspend fun executeWorkflow(
      input: LoadedInProgressAppointmentState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<CompletedAppointmentState>> =
      Either.Right(
        WorkflowResult(
          state = CompletedAppointmentState(CompleteAppointment.of(input.appointment, input.notes)),
          context = context,
        )
      )
  }

  private class PersistAppointmentCompletedStep(
    override val id: String,
    private val appointmentEventPort: AppointmentEventPort,
  ) : UseCaseWorkflow<CompletedAppointmentState, AppointmentCompletedPersistedState>() {

    override suspend fun executeWorkflow(
      input: CompletedAppointmentState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<AppointmentCompletedPersistedState>> =
      appointmentEventPort.handle(AppointmentCompletedEvent(input.appointment.toDto()))
        .toUseCaseEither()
        .map { event -> WorkflowResult(state = AppointmentCompletedPersistedState(event), context = context) }
  }
}

private data class CompleteAppointmentRequest(
  val appointmentId: String,
  val notes: String?,
) : UseCaseCommand

private data class CompleteAppointmentState(
  val appointmentId: String,
  val notes: String?,
) : WorkflowState

private data class LoadedInProgressAppointmentState(
  val appointment: InProgressAppointment,
  val notes: String?,
) : WorkflowState

private data class CompletedAppointmentState(
  val appointment: CompleteAppointment,
) : WorkflowState

private data class AppointmentCompletedPersistedState(
  val event: AppointmentCompletedEvent,
) : WorkflowState

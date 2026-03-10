package io.liquidsoftware.base.booking.application.workflows

import arrow.core.Either
import arrow.core.flatMap
import io.liquidsoftware.base.booking.application.mapper.toDto
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentStartedEvent
import io.liquidsoftware.base.booking.application.port.`in`.StartAppointmentCommand
import io.liquidsoftware.base.booking.application.port.out.AppointmentEventPort
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.domain.InProgressAppointment
import io.liquidsoftware.base.booking.domain.ScheduledAppointment
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

internal class StartAppointmentUseCase(
  private val findAppointmentPort: FindAppointmentPort,
  private val appointmentEventPort: AppointmentEventPort,
) {

  private val useCase = useCase<AppointmentIdRequest> {
    startWith { request -> Either.Right(AppointmentIdState(request.appointmentId)) }
    then(LoadScheduledAppointmentStep("load-scheduled-appointment", findAppointmentPort))
    then(BuildInProgressAppointmentStep("build-in-progress-appointment"))
    then(PersistAppointmentStartedStep("persist-appointment-started", appointmentEventPort))
  }

  suspend fun execute(command: StartAppointmentCommand): Either<ApplicationError, AppointmentStartedEvent> =
    useCase.executeProjected(
      AppointmentIdRequest(command.appointmentId),
      projector = { result ->
        result.requireState<AppointmentStartedPersistedState>("persist-appointment-started").fold(
          { Either.Left(it) },
          { state -> Either.Right(state.event) },
        )
      },
    ).toApplicationUseCaseEither(::mapBookingDomainError)

  private class LoadScheduledAppointmentStep(
    override val id: String,
    private val findAppointmentPort: FindAppointmentPort,
  ) : UseCaseWorkflow<AppointmentIdState, LoadedScheduledAppointmentState>() {

    override suspend fun executeWorkflow(
      input: AppointmentIdState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<LoadedScheduledAppointmentState>> =
      findAppointmentPort.findScheduledById(input.appointmentId)
        .toUseCaseEither()
        .flatMap { appointment ->
          appointment
            ?.let { Either.Right(WorkflowResult(state = LoadedScheduledAppointmentState(it), context = context)) }
            ?: Either.Left(
              UseCaseError.DomainError(
                APPOINTMENT_VALIDATION_CODE,
                "Could not find ready Appointment to start",
              )
            )
        }
  }

  private class BuildInProgressAppointmentStep(
    override val id: String,
  ) : UseCaseWorkflow<LoadedScheduledAppointmentState, InProgressAppointmentState>() {

    override suspend fun executeWorkflow(
      input: LoadedScheduledAppointmentState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<InProgressAppointmentState>> =
      Either.Right(
        WorkflowResult(
          state = InProgressAppointmentState(InProgressAppointment.of(input.appointment)),
          context = context,
        )
      )
  }

  private class PersistAppointmentStartedStep(
    override val id: String,
    private val appointmentEventPort: AppointmentEventPort,
  ) : UseCaseWorkflow<InProgressAppointmentState, AppointmentStartedPersistedState>() {

    override suspend fun executeWorkflow(
      input: InProgressAppointmentState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<AppointmentStartedPersistedState>> =
      appointmentEventPort.handle(AppointmentStartedEvent(input.appointment.toDto()))
        .toUseCaseEither()
        .map { event -> WorkflowResult(state = AppointmentStartedPersistedState(event), context = context) }
  }
}

private data class AppointmentIdRequest(
  val appointmentId: String,
) : UseCaseCommand

private data class AppointmentIdState(
  val appointmentId: String,
) : WorkflowState

private data class LoadedScheduledAppointmentState(
  val appointment: ScheduledAppointment,
) : WorkflowState

private data class InProgressAppointmentState(
  val appointment: InProgressAppointment,
) : WorkflowState

private data class AppointmentStartedPersistedState(
  val event: AppointmentStartedEvent,
) : WorkflowState

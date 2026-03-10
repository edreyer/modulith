package io.liquidsoftware.base.booking.application.workflows

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import io.liquidsoftware.base.booking.application.mapper.toDto
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentCancelledEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentError
import io.liquidsoftware.base.booking.application.port.`in`.CancelAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.CancelAppointmentError
import io.liquidsoftware.base.booking.application.port.out.AppointmentEventPort
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.application.service.AppointmentStateService
import io.liquidsoftware.base.booking.domain.Appointment
import io.liquidsoftware.base.booking.domain.CancelledAppointment
import io.liquidsoftware.common.application.error.ApplicationError
import io.liquidsoftware.common.application.error.toApplicationUseCaseEither
import io.liquidsoftware.common.application.error.toUseCaseApplicationEither
import io.liquidsoftware.common.usecase.Command as UseCaseCommand
import io.liquidsoftware.common.usecase.Workflow as UseCaseWorkflow
import io.liquidsoftware.common.usecase.WorkflowContext
import io.liquidsoftware.common.usecase.WorkflowResult
import io.liquidsoftware.common.usecase.WorkflowState
import io.liquidsoftware.common.usecase.toUseCaseEither
import io.liquidsoftware.common.usecase.useCase
import io.liquidsoftware.workflow.WorkflowError as UseCaseError

internal class CancelAppointmentUseCase(
  private val appointmentStateService: AppointmentStateService,
  private val findAppointmentPort: FindAppointmentPort,
  private val appointmentEventPort: AppointmentEventPort,
) {

  private val useCase = useCase<CancelAppointmentRequest> {
    startWith { request -> Either.Right(CancelAppointmentState(request.appointmentId, request.notes)) }
    then(LoadAppointmentStep("load-appointment-by-id", findAppointmentPort))
    then(CancelAppointmentStep("cancel-appointment-state-transition", appointmentStateService))
    then(PersistAppointmentCancelledStep("persist-appointment-cancelled", appointmentEventPort))
  }

  suspend fun execute(command: CancelAppointmentCommand): Either<ApplicationError, AppointmentCancelledEvent> =
    useCase.executeProjected(
      CancelAppointmentRequest(command.appointmentId, command.notes),
      projector = { result ->
        result.requireState<AppointmentCancelledPersistedState>("persist-appointment-cancelled").fold(
          { Either.Left(it) },
          { state -> Either.Right(state.event) },
        )
      },
    ).toApplicationUseCaseEither(::mapBookingDomainError)

  private class LoadAppointmentStep(
    override val id: String,
    private val findAppointmentPort: FindAppointmentPort,
  ) : UseCaseWorkflow<CancelAppointmentState, LoadedAppointmentState>() {

    override suspend fun executeWorkflow(
      input: CancelAppointmentState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<LoadedAppointmentState>> =
      findAppointmentPort.findById(input.appointmentId)
        .toUseCaseEither()
        .flatMap { appointment ->
          appointment
            ?.let {
              Either.Right(
                WorkflowResult(
                  state = LoadedAppointmentState(appointment = it, notes = input.notes),
                  context = context,
                )
              )
            }
            ?: Either.Left(
              UseCaseError.DomainError(
                APPOINTMENT_NOT_FOUND_CODE,
                "Appointment(${input.appointmentId} not found",
              )
            )
        }
  }

  private class CancelAppointmentStep(
    override val id: String,
    private val appointmentStateService: AppointmentStateService,
  ) : UseCaseWorkflow<LoadedAppointmentState, CancelledAppointmentState>() {

    override suspend fun executeWorkflow(
      input: LoadedAppointmentState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<CancelledAppointmentState>> =
      either<ApplicationError, CancelledAppointment> {
        appointmentStateService.cancel(input.appointment)
      }
        .toUseCaseApplicationEither { applicationError ->
          when (applicationError) {
            is CancelAppointmentError -> UseCaseError.DomainError(CANCEL_APPOINTMENT_CODE, applicationError.message)
            else -> null
          }
        }
        .map { appointment -> WorkflowResult(state = CancelledAppointmentState(appointment), context = context) }
  }

  private class PersistAppointmentCancelledStep(
    override val id: String,
    private val appointmentEventPort: AppointmentEventPort,
  ) : UseCaseWorkflow<CancelledAppointmentState, AppointmentCancelledPersistedState>() {

    override suspend fun executeWorkflow(
      input: CancelledAppointmentState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<AppointmentCancelledPersistedState>> =
      appointmentEventPort.handle(AppointmentCancelledEvent(input.appointment.toDto()))
        .toUseCaseEither()
        .map { event -> WorkflowResult(state = AppointmentCancelledPersistedState(event), context = context) }
  }
}

private data class CancelAppointmentRequest(
  val appointmentId: String,
  val notes: String?,
) : UseCaseCommand

private data class CancelAppointmentState(
  val appointmentId: String,
  val notes: String?,
) : WorkflowState

private data class LoadedAppointmentState(
  val appointment: Appointment,
  val notes: String?,
) : WorkflowState

private data class CancelledAppointmentState(
  val appointment: CancelledAppointment,
) : WorkflowState

private data class AppointmentCancelledPersistedState(
  val event: AppointmentCancelledEvent,
) : WorkflowState

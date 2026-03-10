package io.liquidsoftware.base.booking.application.workflows

import arrow.core.Either
import arrow.core.raise.either
import io.liquidsoftware.base.booking.application.mapper.toDto
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentScheduledEvent
import io.liquidsoftware.base.booking.application.port.`in`.ScheduleAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.WorkOrderDtoIn
import io.liquidsoftware.base.booking.application.port.out.AppointmentEventPort
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.application.service.AvailabilityService
import io.liquidsoftware.base.booking.domain.Appointment
import io.liquidsoftware.base.booking.domain.ReadyWorkOrder
import io.liquidsoftware.base.booking.domain.ScheduledAppointment
import io.liquidsoftware.common.application.error.ApplicationError
import io.liquidsoftware.common.application.error.toApplicationUseCaseEither
import io.liquidsoftware.common.types.toErrString
import io.liquidsoftware.common.usecase.Command as UseCaseCommand
import io.liquidsoftware.common.usecase.Workflow as UseCaseWorkflow
import io.liquidsoftware.common.usecase.WorkflowContext
import io.liquidsoftware.common.usecase.WorkflowResult
import io.liquidsoftware.common.usecase.WorkflowState
import io.liquidsoftware.common.usecase.toUseCaseEither
import io.liquidsoftware.common.usecase.useCase
import io.liquidsoftware.workflow.WorkflowError as UseCaseError
import java.time.LocalDateTime

internal class ScheduleAppointmentUseCase(
  private val availabilityService: AvailabilityService,
  private val findAppointmentPort: FindAppointmentPort,
  private val appointmentEventPort: AppointmentEventPort,
) {

  private val useCase = useCase<ScheduleAppointmentRequest> {
    startWith { request ->
      Either.Right(
        ScheduleAppointmentState(
          userId = request.userId,
          scheduledTime = request.scheduledTime,
          duration = request.duration,
          workOrder = request.workOrder,
        )
      )
    }
    then(LoadAppointmentsForScheduleStep("load-appointments-for-availability", findAppointmentPort))
    then(EnsureRequestedTimeAvailableStep("ensure-requested-time-available", availabilityService))
    then(BuildScheduledAppointmentStep("build-scheduled-appointment"))
    then(PersistAppointmentScheduledStep("persist-appointment-scheduled", appointmentEventPort))
  }

  suspend fun execute(command: ScheduleAppointmentCommand): Either<ApplicationError, AppointmentScheduledEvent> =
    useCase.executeProjected(
      ScheduleAppointmentRequest(
        userId = command.userId,
        scheduledTime = command.scheduledTime,
        duration = command.duration,
        workOrder = command.workOrder,
      ),
      projector = { result ->
        result.requireState<AppointmentScheduledPersistedState>("persist-appointment-scheduled").fold(
          { Either.Left(it) },
          { state -> Either.Right(state.event) },
        )
      },
    ).toApplicationUseCaseEither(::mapBookingDomainError)

  private class LoadAppointmentsForScheduleStep(
    override val id: String,
    private val findAppointmentPort: FindAppointmentPort,
  ) : UseCaseWorkflow<ScheduleAppointmentState, ScheduleAppointmentAvailabilityState>() {

    override suspend fun executeWorkflow(
      input: ScheduleAppointmentState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<ScheduleAppointmentAvailabilityState>> =
      findAppointmentPort.findAllForAvailability(input.scheduledTime.toLocalDate())
        .toUseCaseEither()
        .map { appointments ->
          WorkflowResult(
            state = ScheduleAppointmentAvailabilityState(
              userId = input.userId,
              scheduledTime = input.scheduledTime,
              duration = input.duration,
              workOrder = input.workOrder,
              appointmentsForDay = appointments,
            ),
            context = context,
          )
        }
  }

  private class EnsureRequestedTimeAvailableStep(
    override val id: String,
    private val availabilityService: AvailabilityService,
  ) : UseCaseWorkflow<ScheduleAppointmentAvailabilityState, ScheduleAppointmentAvailabilityState>() {

    override suspend fun executeWorkflow(
      input: ScheduleAppointmentAvailabilityState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<ScheduleAppointmentAvailabilityState>> =
      if (availabilityService.isTimeAvailable(input.appointmentsForDay, input.scheduledTime.toLocalTime())) {
        Either.Right(WorkflowResult(state = input, context = context))
      } else {
        Either.Left(
          UseCaseError.DomainError(
            DATE_TIME_UNAVAILABLE_CODE,
            "'${input.scheduledTime.toLocalTime()}' is no longer available.",
          )
        )
      }
  }

  private class BuildScheduledAppointmentStep(
    override val id: String,
  ) : UseCaseWorkflow<ScheduleAppointmentAvailabilityState, ScheduledAppointmentState>() {

    override suspend fun executeWorkflow(
      input: ScheduleAppointmentAvailabilityState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<ScheduledAppointmentState>> =
      either {
        val workOrder = ReadyWorkOrder.of(
          service = input.workOrder.service,
          notes = input.workOrder.notes,
        )
        ScheduledAppointment.of(
          userId = input.userId,
          scheduledTime = input.scheduledTime,
          duration = input.duration,
          workOrder = workOrder,
        )
      }.fold(
        {
          Either.Left(UseCaseError.DomainError(APPOINTMENT_VALIDATION_CODE, it.toErrString()))
        },
        { appointment ->
          Either.Right(WorkflowResult(state = ScheduledAppointmentState(appointment), context = context))
        },
      )
  }

  private class PersistAppointmentScheduledStep(
    override val id: String,
    private val appointmentEventPort: AppointmentEventPort,
  ) : UseCaseWorkflow<ScheduledAppointmentState, AppointmentScheduledPersistedState>() {

    override suspend fun executeWorkflow(
      input: ScheduledAppointmentState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<AppointmentScheduledPersistedState>> =
      appointmentEventPort.handle(AppointmentScheduledEvent(input.appointment.toDto()))
        .toUseCaseEither()
        .map { event -> WorkflowResult(state = AppointmentScheduledPersistedState(event), context = context) }
  }
}

private data class ScheduleAppointmentRequest(
  val userId: String,
  val scheduledTime: LocalDateTime,
  val duration: Long,
  val workOrder: WorkOrderDtoIn,
) : UseCaseCommand

private data class ScheduleAppointmentState(
  val userId: String,
  val scheduledTime: LocalDateTime,
  val duration: Long,
  val workOrder: WorkOrderDtoIn,
) : WorkflowState

private data class ScheduleAppointmentAvailabilityState(
  val userId: String,
  val scheduledTime: LocalDateTime,
  val duration: Long,
  val workOrder: WorkOrderDtoIn,
  val appointmentsForDay: List<Appointment>,
) : WorkflowState

private data class ScheduledAppointmentState(
  val appointment: ScheduledAppointment,
) : WorkflowState

private data class AppointmentScheduledPersistedState(
  val event: AppointmentScheduledEvent,
) : WorkflowState

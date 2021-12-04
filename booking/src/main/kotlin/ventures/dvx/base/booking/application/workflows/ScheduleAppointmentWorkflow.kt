package ventures.dvx.base.booking.application.workflows

import arrow.core.computations.ResultEffect.bind
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import ventures.dvx.base.booking.application.config.BookingBridgekeeperConfig
import ventures.dvx.base.booking.application.port.`in`.AppointmentDto.AppointmentDtoData
import ventures.dvx.base.booking.application.port.`in`.AppointmentDto.ScheduledAppointmentDto
import ventures.dvx.base.booking.application.port.`in`.AppointmentScheduledEvent
import ventures.dvx.base.booking.application.port.`in`.ScheduleAppointmentCommand
import ventures.dvx.base.booking.application.port.`in`.ScheduleAppointmentError.AppointmentValidationError
import ventures.dvx.base.booking.application.port.`in`.ScheduleAppointmentError.DateTimeUnavailableError
import ventures.dvx.base.booking.application.port.out.FindAppointmentPort
import ventures.dvx.base.booking.application.service.AppointmentStateService
import ventures.dvx.base.booking.application.service.AvailabilityService
import ventures.dvx.base.booking.domain.Appointment
import ventures.dvx.base.booking.domain.DraftAppointment
import ventures.dvx.base.booking.domain.ScheduledAppointment
import ventures.dvx.base.user.application.port.`in`.FindUserByIdQuery
import ventures.dvx.base.user.application.port.`in`.UserFoundEvent
import ventures.dvx.base.user.bridgekeeper.UserSecured
import ventures.dvx.bridgekeeper.BridgeKeeper
import ventures.dvx.common.events.EventPublisher
import ventures.dvx.common.security.ExecutionContext
import ventures.dvx.common.types.toErrString
import ventures.dvx.common.workflow.BaseSafeSecureWorkflow
import ventures.dvx.common.workflow.WorkflowDispatcher
import java.time.LocalTime
import javax.annotation.PostConstruct

@Component
internal class ScheduleAppointmentWorkflow(
  private val eventPublisher: EventPublisher,
  private val apptStateService: AppointmentStateService,
  private val availabilityService: AvailabilityService,
  private val findAppts: FindAppointmentPort,
  override val ec: ExecutionContext,
  @Qualifier(BookingBridgekeeperConfig.BOOKING_BRIDGE_KEEPER) override val bk: BridgeKeeper
) : BaseSafeSecureWorkflow<ScheduleAppointmentCommand, AppointmentScheduledEvent>(),
  UserSecured<ScheduleAppointmentCommand> {

  @PostConstruct
  fun registerWithDispatcher() = WorkflowDispatcher.registerCommandHandler(this)

  override suspend fun userMatchingFn(request: ScheduleAppointmentCommand): Boolean {
    return WorkflowDispatcher.dispatch<UserFoundEvent>(FindUserByIdQuery(request.userId))
        .fold(
          { ec.getCurrentUser().username == it.userDto.email },
          { false }
        )
  }

  override suspend fun execute(request: ScheduleAppointmentCommand): AppointmentScheduledEvent {
    // business invariant we must check
    val todaysAppts = findAppts.findAll(request.startTime.toLocalDate())
    checkTimeAvailable(todaysAppts, request.startTime.toLocalTime())

    return DraftAppointment.of(request.userId, request.startTime, request.duration)
      .map { apptStateService.schedule(it) }
      .fold({
        Result.failure(AppointmentValidationError(it.toErrString()))
      }, {
        Result.success(eventPublisher.publish(AppointmentScheduledEvent(it.toDto())))
      }).bind()
  }

  private suspend fun checkTimeAvailable(
    todaysAppts: List<Appointment>,
    startTime: LocalTime,
  ) {
    if (!availabilityService.isTimeAvailable(todaysAppts, startTime)) {
      throw DateTimeUnavailableError("'$startTime' is no longer available.")
    }
  }

  suspend fun ScheduledAppointment.toDto(): ScheduledAppointmentDto =
    ScheduledAppointmentDto(AppointmentDtoData(
      this.id.value,
      this.userId.value,
      this.startTime,
      this.duration.toMinutes())
    )
}

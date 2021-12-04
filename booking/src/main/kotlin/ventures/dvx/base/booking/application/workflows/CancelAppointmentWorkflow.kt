package ventures.dvx.base.booking.application.workflows

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import ventures.dvx.base.booking.application.config.BookingBridgekeeperConfig
import ventures.dvx.base.booking.application.port.`in`.AppointmentCancelledEvent
import ventures.dvx.base.booking.application.port.`in`.AppointmentDto.AppointmentDtoData
import ventures.dvx.base.booking.application.port.`in`.AppointmentDto.CancelledAppointmentDto
import ventures.dvx.base.booking.application.port.`in`.CancelAppointmentCommand
import ventures.dvx.base.booking.application.port.`in`.ScheduleAppointmentError
import ventures.dvx.base.booking.application.port.out.FindAppointmentPort
import ventures.dvx.base.booking.application.service.AppointmentStateService
import ventures.dvx.base.booking.domain.CancelledAppointment
import ventures.dvx.base.user.application.port.`in`.FindUserByEmailQuery
import ventures.dvx.base.user.application.port.`in`.UserFoundEvent
import ventures.dvx.base.user.bridgekeeper.UserSecured
import ventures.dvx.bridgekeeper.BridgeKeeper
import ventures.dvx.common.events.EventPublisher
import ventures.dvx.common.security.ExecutionContext
import ventures.dvx.common.workflow.BaseSafeSecureWorkflow
import ventures.dvx.common.workflow.WorkflowDispatcher
import javax.annotation.PostConstruct

@Component
internal class CancelAppointmentWorkflow(
  private val eventPublisher: EventPublisher,
  private val apptStateService: AppointmentStateService,
  private val findAppts: FindAppointmentPort,
  override val ec: ExecutionContext,
  @Qualifier(BookingBridgekeeperConfig.BOOKING_BRIDGE_KEEPER) override val bk: BridgeKeeper
) : BaseSafeSecureWorkflow<CancelAppointmentCommand, AppointmentCancelledEvent>(),
  UserSecured<CancelAppointmentCommand> {

  @PostConstruct
  fun registerWithDispatcher() = WorkflowDispatcher.registerCommandHandler(this)

  override suspend fun userMatchingFn(request: CancelAppointmentCommand): Boolean {
    return WorkflowDispatcher.dispatch<UserFoundEvent>(FindUserByEmailQuery(ec.getCurrentUser().username))
        .fold({
            findAppts.findById(request.appointmentId)
            ?.let { appt -> appt.userId.value == it.userDto.id }
            ?: false
          }, {
            false
          }
        )
  }

  override suspend fun execute(request: CancelAppointmentCommand): AppointmentCancelledEvent {
    return findAppts.findById(request.appointmentId)
      ?.let { apptStateService.cancel(it) }
      ?.let { eventPublisher.publish(AppointmentCancelledEvent(it.toDto()))}
      ?: throw ScheduleAppointmentError.CancelAppointmentError("Failed to cancel appt ID='${request.appointmentId}'")
  }

  suspend fun CancelledAppointment.toDto(): CancelledAppointmentDto =
    CancelledAppointmentDto(AppointmentDtoData(
      this.id.value,
      this.userId.value,
      this.startTime,
      this.duration.toMinutes()),
      this.cancelDate
    )
}

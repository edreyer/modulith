package io.liquidsoftware.base.booking.application.workflows

import io.liquidsoftware.base.booking.application.port.`in`.AppointmentCompletedEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentValidationError
import io.liquidsoftware.base.booking.application.port.`in`.CompleteAppointmentCommand
import io.liquidsoftware.base.booking.application.port.out.AppointmentEventPort
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.application.port.out.toDto
import io.liquidsoftware.base.booking.domain.CompleteAppointment
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
internal class CompleteAppointmentWorkflow(
  private val findAppointmentPort: FindAppointmentPort,
  private val appointmentEventPort: AppointmentEventPort,
) : BaseSafeWorkflow<CompleteAppointmentCommand, AppointmentCompletedEvent>() {

  @PostConstruct
  fun registerWithDispatcher() = WorkflowDispatcher.registerCommandHandler(this)

  override suspend fun execute(request: CompleteAppointmentCommand): AppointmentCompletedEvent {
    // business invariant we must check
    return findAppointmentPort.findStartedById(request.appointmentId)
      ?.let { CompleteAppointment.of(it, request.notes) }
      ?.let {
        appointmentEventPort.handle(AppointmentCompletedEvent(it.toDto()))
      }
      ?: throw AppointmentValidationError("Could not find started Appointment to start")
  }

}

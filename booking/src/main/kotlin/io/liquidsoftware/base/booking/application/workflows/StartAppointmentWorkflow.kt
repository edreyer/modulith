package io.liquidsoftware.base.booking.application.workflows

import arrow.core.raise.Raise
import io.liquidsoftware.base.booking.application.mapper.toDto
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentStartedEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentValidationError
import io.liquidsoftware.base.booking.application.port.`in`.StartAppointmentCommand
import io.liquidsoftware.base.booking.application.port.out.AppointmentEventPort
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.domain.InProgressAppointment
import io.liquidsoftware.common.ext.ensureNotNull
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowError
import io.liquidsoftware.common.workflow.WorkflowRegistry
import org.springframework.stereotype.Component

@Component
internal class StartAppointmentWorkflow(
  private val findAppointmentPort: FindAppointmentPort,
  private val appointmentEventPort: AppointmentEventPort,
) : BaseSafeWorkflow<StartAppointmentCommand, AppointmentStartedEvent>() {

  override fun registerWithDispatcher() = WorkflowRegistry.registerCommandHandler(this)

  context(_: Raise<WorkflowError>)
  override suspend fun execute(request: StartAppointmentCommand): AppointmentStartedEvent {
    // business invariant we must check
    return ensureNotNull(findAppointmentPort.findScheduledById(request.appointmentId)) {
      AppointmentValidationError("Could not find ready Appointment to start")
    }
      .let { InProgressAppointment.of(it) }
      .let { appointmentEventPort.handle(AppointmentStartedEvent(it.toDto())) }
  }

}

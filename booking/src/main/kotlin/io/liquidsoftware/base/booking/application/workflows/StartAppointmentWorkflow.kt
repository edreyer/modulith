package io.liquidsoftware.base.booking.application.workflows

import arrow.core.continuations.EffectScope
import arrow.core.continuations.ensureNotNull
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentStartedEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentValidationError
import io.liquidsoftware.base.booking.application.port.`in`.StartAppointmentCommand
import io.liquidsoftware.base.booking.application.port.out.AppointmentEventPort
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.application.port.out.toDto
import io.liquidsoftware.base.booking.domain.InProgressAppointment
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import io.liquidsoftware.common.workflow.WorkflowError
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
internal class StartAppointmentWorkflow(
  private val findAppointmentPort: FindAppointmentPort,
  private val appointmentEventPort: AppointmentEventPort,
) : BaseSafeWorkflow<StartAppointmentCommand, AppointmentStartedEvent>() {

  @PostConstruct
  fun registerWithDispatcher() = WorkflowDispatcher.registerCommandHandler(this)

  context(EffectScope<WorkflowError>)
  override suspend fun execute(request: StartAppointmentCommand): AppointmentStartedEvent {
    // business invariant we must check
    return ensureNotNull(findAppointmentPort.findScheduledById(request.appointmentId)) {
      AppointmentValidationError("Could not find ready Appointment to start")
    }
      .let { InProgressAppointment.of(it) }
      .let { appointmentEventPort.handle(AppointmentStartedEvent(it.toDto())) }
  }

}

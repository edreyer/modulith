package io.liquidsoftware.base.booking.application.workflows

import arrow.core.continuations.EffectScope
import arrow.core.continuations.ensureNotNull
import io.liquidsoftware.base.booking.application.mapper.toDto
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentNotFoundError
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentPaidEvent
import io.liquidsoftware.base.booking.application.port.`in`.PayAppointmentCommand
import io.liquidsoftware.base.booking.application.port.out.AppointmentEventPort
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.domain.PaidAppointment
import io.liquidsoftware.base.payment.application.port.`in`.MakePaymentCommand
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMadeEvent
import io.liquidsoftware.common.ext.getOrShift
import io.liquidsoftware.common.logging.LoggerDelegate
import io.liquidsoftware.common.security.ExecutionContext
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import io.liquidsoftware.common.workflow.WorkflowError
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
internal class PayAppointmentWorkflow(
  private val ec: ExecutionContext,
  private val findAppointmentPort: FindAppointmentPort,
  private val appointmentEventPort: AppointmentEventPort,
) : BaseSafeWorkflow<PayAppointmentCommand, AppointmentPaidEvent>() {

  private val log by LoggerDelegate()

  @PostConstruct
  fun registerWithDispatcher() = WorkflowDispatcher.registerCommandHandler(this)

  context(EffectScope<WorkflowError>)
  override suspend fun execute(request: PayAppointmentCommand): AppointmentPaidEvent {
    // 1) Ensure appt is in correct state
    val completeAppt = ensureNotNull(findAppointmentPort.findCompletedById(request.appointmentId)) {
      AppointmentNotFoundError("Appointment(${request.appointmentId} must be Completed")
    }

    // 2) attempt a payment on the appt
    return WorkflowDispatcher.dispatch<PaymentMadeEvent>(MakePaymentCommand(
      userId = ec.getCurrentUser().id,
      paymentMethodId = request.paymentMethodId,
      amount = completeAppt.totalDue()
    ))
      .map { PaidAppointment.of(completeAppt, it.paymentDto.paymentId).getOrShift() }
      .map { appointmentEventPort.handle(AppointmentPaidEvent(it.toDto())) }
      .tapLeft { ex -> log.error("Failed to make payment on appt ${request.appointmentId}", ex) }
      .getOrShift()

  }

}

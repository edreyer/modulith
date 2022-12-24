package io.liquidsoftware.base.payment.application.workflows

import arrow.core.continuations.EffectScope
import io.liquidsoftware.base.payment.application.mapper.toDto
import io.liquidsoftware.base.payment.application.port.`in`.AddPaymentMethodCommand
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodAddedEvent
import io.liquidsoftware.base.payment.application.port.out.PaymentEventPort
import io.liquidsoftware.base.payment.domain.ActivePaymentMethod
import io.liquidsoftware.common.ext.getOrShift
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import io.liquidsoftware.common.workflow.WorkflowError
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

@Component
internal class AddPaymentMethodWorkflow(
  val paymentEventPort: PaymentEventPort
) : BaseSafeWorkflow<AddPaymentMethodCommand, PaymentMethodAddedEvent>() {

  @PostConstruct
  fun registerWithDispatcher() = WorkflowDispatcher.registerCommandHandler(this)

  context(EffectScope<WorkflowError>)
  override suspend fun execute(request: AddPaymentMethodCommand): PaymentMethodAddedEvent {
    return ActivePaymentMethod.of(
      userId = request.paymentMethod.userId,
      stripePaymentMethodId = request.paymentMethod.stripePaymentMethodId,
      lastFour = request.paymentMethod.lastFour
    )
      .getOrShift()
      .let { paymentEventPort.handle(PaymentMethodAddedEvent(it.toDto())) }
  }
}

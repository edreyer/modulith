package io.liquidsoftware.base.payment.application.workflows

import arrow.core.raise.Raise
import arrow.core.raise.either
import io.liquidsoftware.base.payment.application.mapper.toDto
import io.liquidsoftware.base.payment.application.port.`in`.AddPaymentMethodCommand
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodAddedEvent
import io.liquidsoftware.base.payment.application.port.out.PaymentEventPort
import io.liquidsoftware.base.payment.domain.ActivePaymentMethod
import io.liquidsoftware.common.ext.getOrRaise
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

  context(Raise<WorkflowError>)
  override suspend fun execute(request: AddPaymentMethodCommand): PaymentMethodAddedEvent {
    return either {
      ActivePaymentMethod.of(
        userId = request.paymentMethod.userId,
        stripePaymentMethodId = request.paymentMethod.stripePaymentMethodId,
        lastFour = request.paymentMethod.lastFour
      )
    }
      .getOrRaise()
      .let { paymentEventPort.handle(PaymentMethodAddedEvent(it.toDto())) }
    }
}


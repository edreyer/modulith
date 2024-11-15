package io.liquidsoftware.base.payment.application.workflows

import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import io.liquidsoftware.base.payment.PaymentMethodId
import io.liquidsoftware.base.payment.application.mapper.toDto
import io.liquidsoftware.base.payment.application.port.`in`.MakePaymentCommand
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMadeEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodNotFoundError
import io.liquidsoftware.base.payment.application.port.out.FindPaymentMethodPort
import io.liquidsoftware.base.payment.application.port.out.PaymentEventPort
import io.liquidsoftware.base.payment.application.service.StripeService
import io.liquidsoftware.base.payment.domain.Payment
import io.liquidsoftware.base.user.UserId
import io.liquidsoftware.common.ext.getOrRaise
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowError
import io.liquidsoftware.common.workflow.WorkflowRegistry
import org.springframework.stereotype.Component

@Component
internal class MakePaymentWorkflow(
  val findPaymentMethodPort: FindPaymentMethodPort,
  val paymentEventPort: PaymentEventPort,
  val stripeService: StripeService

) : BaseSafeWorkflow<MakePaymentCommand, PaymentMadeEvent>() {

  override fun registerWithDispatcher() = WorkflowRegistry.registerCommandHandler(this)

  context(Raise<WorkflowError>)
  override suspend fun execute(request: MakePaymentCommand): PaymentMadeEvent {
    val pmId = either { PaymentMethodId.of(request.paymentMethodId) }.getOrRaise()
    val userId = either { UserId.of(request.userId) }.getOrRaise()
    val pm = ensureNotNull(findPaymentMethodPort.findByPaymentMethodId(pmId, userId)) {
      PaymentMethodNotFoundError(request.paymentMethodId)
    }

    return stripeService.makePayment(pm, request.amount)
      .let {
        either {
          Payment.of(
            paymentMethodId = it.paymentMethod.id.value,
            userId = request.userId,
            amount = it.amount
          )
        }.getOrRaise()
      }
      .let { paymentEventPort.handle(PaymentMadeEvent(it.toDto())) }
  }
}

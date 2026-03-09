package io.liquidsoftware.base.payment.application.workflows

import arrow.core.raise.Raise
import io.liquidsoftware.base.payment.application.port.`in`.MakePaymentCommand
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMadeEvent
import io.liquidsoftware.base.payment.application.port.out.FindPaymentMethodPort
import io.liquidsoftware.base.payment.application.port.out.PaymentEventPort
import io.liquidsoftware.base.payment.application.service.StripeService
import arrow.core.raise.context.bind
import io.liquidsoftware.common.security.ExecutionContext
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowError
import io.liquidsoftware.common.workflow.WorkflowRegistry
import org.springframework.stereotype.Component

@Component
internal class MakePaymentWorkflow(
  val ec: ExecutionContext,
  val findPaymentMethodPort: FindPaymentMethodPort,
  val paymentEventPort: PaymentEventPort,
  val stripeService: StripeService

) : BaseSafeWorkflow<MakePaymentCommand, PaymentMadeEvent>() {

  private val useCase = MakePaymentUseCase(
    executionContext = ec,
    findPaymentMethodPort = findPaymentMethodPort,
    paymentEventPort = paymentEventPort,
    stripeService = stripeService,
  )

  override fun registerWithDispatcher() = WorkflowRegistry.registerCommandHandler(this)

  context(_: Raise<WorkflowError>)
  override suspend fun execute(request: MakePaymentCommand): PaymentMadeEvent =
    useCase.execute(request).bind()
}

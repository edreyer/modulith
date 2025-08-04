package io.liquidsoftware.base.payment.application.service

import arrow.core.raise.Raise
import io.liquidsoftware.base.payment.application.port.`in`.PaymentDeclinedError
import io.liquidsoftware.base.payment.domain.PaymentMethod
import io.liquidsoftware.common.ext.raise
import io.liquidsoftware.common.workflow.WorkflowError
import org.springframework.stereotype.Service

@Service
internal class StripeService {

  data class SuccessfulPayment(val paymentMethod: PaymentMethod, val amount: Long)

  context(_: Raise<WorkflowError>)
  suspend fun makePayment(paymentMethod: PaymentMethod, amount: Long): SuccessfulPayment =
    if (amount < 10000) {
      SuccessfulPayment(paymentMethod, amount)
    } else {
      raise(PaymentDeclinedError("Insufficient Funds"))
    }
}

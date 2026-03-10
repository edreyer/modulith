package io.liquidsoftware.base.payment.application.service

import arrow.core.raise.Raise
import arrow.core.raise.context.ensure
import io.liquidsoftware.base.payment.application.port.`in`.PaymentDeclinedError
import io.liquidsoftware.base.payment.domain.PaymentMethod
import org.springframework.stereotype.Service

@Service
internal class StripeService {

  data class SuccessfulPayment(val paymentMethod: PaymentMethod, val amount: Long)

  context(_: Raise<PaymentDeclinedError>)
  suspend fun makePayment(paymentMethod: PaymentMethod, amount: Long): SuccessfulPayment {
    ensure(amount < 10000) { PaymentDeclinedError("Insufficient Funds") }
    return SuccessfulPayment(paymentMethod, amount)
  }
}

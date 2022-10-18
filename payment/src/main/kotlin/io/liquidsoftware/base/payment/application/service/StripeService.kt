package io.liquidsoftware.base.payment.application.service

import io.liquidsoftware.base.payment.application.port.`in`.PaymentDeclinedError
import io.liquidsoftware.base.payment.domain.PaymentMethod
import org.springframework.stereotype.Service

@Service
internal class StripeService {

  data class SuccessfulPayment(val paymentMethod: PaymentMethod, val amount: Long)

  suspend fun makePayment(paymentMethod: PaymentMethod, amount: Long): Result<SuccessfulPayment> =
    if (amount < 10000) {
      Result.success(SuccessfulPayment(paymentMethod, amount))
    } else {
      Result.failure(PaymentDeclinedError("Insufficient Funds"))
    }
}

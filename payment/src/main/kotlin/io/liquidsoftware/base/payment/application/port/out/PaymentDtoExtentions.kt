package io.liquidsoftware.base.payment.application.port.out

import io.liquidsoftware.base.payment.application.port.`in`.PaymentDtoOut
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodDtoOut
import io.liquidsoftware.base.payment.domain.Payment
import io.liquidsoftware.base.payment.domain.PaymentMethod

internal fun PaymentMethod.toDto(): PaymentMethodDtoOut =
  PaymentMethodDtoOut(
    paymentMethodId = id.value,
    userId = userId.value,
    stripePaymentMethodId = stripePaymentMethodId.value,
    lastFour = lastFour.value
  )

internal fun Payment.toDto(): PaymentDtoOut =
  PaymentDtoOut(
    paymentId = id.value,
    paymentMethodId = paymentMethodId.value,
    userId = userId.value,
    amount = amount.value
  )

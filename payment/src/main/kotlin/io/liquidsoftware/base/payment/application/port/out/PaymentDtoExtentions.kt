package io.liquidsoftware.base.payment.application.port.out

import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodDtoOut
import io.liquidsoftware.base.payment.domain.PaymentMethod

internal fun PaymentMethod.toDto(): PaymentMethodDtoOut =
  PaymentMethodDtoOut(
    paymentMethodId = id.value,
    userId = userId.value,
    stripePaymentMethodId = stripePaymentMethodId.value,
    lastFour = lastFour.value
  )

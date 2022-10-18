package io.liquidsoftware.base.payment.application.port.out

import io.liquidsoftware.base.payment.PaymentMethodId
import io.liquidsoftware.base.payment.domain.PaymentMethod
import io.liquidsoftware.base.user.UserId

internal interface FindPaymentMethodPort {

  suspend fun findByPaymentMethodId(paymentMethodId: PaymentMethodId, userId: UserId): PaymentMethod?

}

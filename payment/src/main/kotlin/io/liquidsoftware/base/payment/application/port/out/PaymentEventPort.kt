package io.liquidsoftware.base.payment.application.port.out

import io.liquidsoftware.base.payment.application.port.`in`.PaymentEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodEvent

internal interface PaymentEventPort {

  suspend fun <T: PaymentMethodEvent> handle(event: T): T

  suspend fun <T: PaymentEvent> handle(event: T): T

}

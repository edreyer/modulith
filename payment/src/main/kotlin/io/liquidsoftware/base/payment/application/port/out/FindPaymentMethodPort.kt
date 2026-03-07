package io.liquidsoftware.base.payment.application.port.out

import arrow.core.Either
import io.liquidsoftware.base.payment.PaymentMethodId
import io.liquidsoftware.base.payment.domain.PaymentMethod
import io.liquidsoftware.base.user.UserId
import io.liquidsoftware.common.workflow.WorkflowError

internal interface FindPaymentMethodPort {

  suspend fun findByPaymentMethodId(
    paymentMethodId: PaymentMethodId,
    userId: UserId
  ): Either<WorkflowError, PaymentMethod?>

}

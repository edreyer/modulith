package io.liquidsoftware.base.payment.application.port.out

import arrow.core.Either
import io.liquidsoftware.base.payment.application.port.`in`.PaymentEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodEvent
import io.liquidsoftware.common.workflow.WorkflowError

internal interface PaymentEventPort {

  suspend fun <T: PaymentMethodEvent> handle(event: T): Either<WorkflowError, T>

  suspend fun <T: PaymentEvent> handle(event: T): Either<WorkflowError, T>


}

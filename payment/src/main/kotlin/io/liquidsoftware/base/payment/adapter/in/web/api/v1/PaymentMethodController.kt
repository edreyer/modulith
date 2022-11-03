package io.liquidsoftware.base.payment.adapter.`in`.web.api.v1

import io.liquidsoftware.base.payment.adapter.`in`.web.V1PaymentPaths
import io.liquidsoftware.base.payment.application.port.`in`.AddPaymentMethodCommand
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodAddedEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodDtoIn
import io.liquidsoftware.common.web.ControllerSupport
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

data class PaymentMethodError(val error:String)

@RestController
class PaymentMethodController() : ControllerSupport {

  @PostMapping(value = [V1PaymentPaths.PAYMENT_METHODS])
  suspend fun addPaymentMethod(@RequestBody paymentMethod: PaymentMethodDtoIn) =
    WorkflowDispatcher.dispatch<PaymentMethodAddedEvent>(
      AddPaymentMethodCommand(paymentMethod)
    )
      .throwIfSpringError()
      .fold(
        {
          ResponseEntity.internalServerError()
            .body(PaymentMethodError("Add Payment Method Error: ${it.message}"))
        },
        { ResponseEntity.ok(it.paymentMethodDto) }
      )

}

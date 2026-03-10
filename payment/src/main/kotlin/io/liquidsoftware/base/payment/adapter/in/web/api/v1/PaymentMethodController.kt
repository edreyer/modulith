package io.liquidsoftware.base.payment.adapter.`in`.web.api.v1

import io.liquidsoftware.base.payment.adapter.`in`.web.V1PaymentPaths
import io.liquidsoftware.base.payment.application.port.`in`.AddPaymentMethodCommand
import io.liquidsoftware.base.payment.application.port.`in`.PaymentApi
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodDtoIn
import io.liquidsoftware.common.application.error.ApplicationError
import io.liquidsoftware.common.security.ExecutionContext
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

data class PaymentMethodError(val error:String)

@RestController
class PaymentMethodController(
  private val ec: ExecutionContext,
  private val paymentApi: PaymentApi,
) {

  @PostMapping(value = [V1PaymentPaths.PAYMENT_METHODS])
  suspend fun addPaymentMethod(@RequestBody paymentMethod: PaymentMethodDtoIn) =
    paymentApi.addPaymentMethod(
      AddPaymentMethodCommand(
        userId = ec.getCurrentUser().id,
        stripePaymentMethodId = paymentMethod.stripePaymentMethodId,
        lastFour = paymentMethod.lastFour
      )
    ).fold(
      { error ->
        when (error) {
          is ApplicationError.Validation ->
            ResponseEntity.badRequest().body(PaymentMethodError(error.message))
          else ->
            ResponseEntity.internalServerError()
              .body(PaymentMethodError("Add Payment Method Error: ${error.message}"))
        }
      },
      { ResponseEntity.ok(it.paymentMethodDto) }
    )

}

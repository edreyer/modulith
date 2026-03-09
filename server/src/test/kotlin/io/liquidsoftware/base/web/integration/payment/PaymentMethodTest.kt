package io.liquidsoftware.base.web.integration.payment

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodDtoOut
import io.liquidsoftware.base.test.BaseWebTest
import org.junit.jupiter.api.Test

class PaymentMethodTest : BaseWebTest() {

  @Test
  fun `authenticated user owns created payment method even if payload supplies userId`() {
    val email = "payment-user@liquidsoftware.io"
    val accessToken = authorizeUser(email, "5125552222").accessToken
    val currentUser = findUserByEmail(email)

    val paymentMethod = post(
      "/api/v1/payment-methods",
      mapOf(
        "userId" to "u_foreign-user",
        "stripePaymentMethodId" to "stripe-pm",
        "lastFour" to "1234"
      ),
      accessToken
    )
      .then()
      .statusCode(200)
      .extract()
      .`as`(PaymentMethodDtoOut::class.java)

    assertThat(paymentMethod.userId).isEqualTo(currentUser.id)
    assertThat(paymentMethod.userId).isNotEqualTo("u_foreign-user")
  }
}

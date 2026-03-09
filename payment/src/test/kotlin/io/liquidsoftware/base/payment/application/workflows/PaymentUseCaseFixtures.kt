package io.liquidsoftware.base.payment.application.workflows

import arrow.core.Either
import arrow.core.raise.either
import io.liquidsoftware.base.payment.domain.ActivePaymentMethod
import io.liquidsoftware.base.payment.application.port.out.PaymentEventPort
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodEvent
import io.liquidsoftware.common.security.UserDetailsWithId
import io.liquidsoftware.common.workflow.WorkflowError
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User

internal fun authenticatePaymentUser(userId: String) {
  val principal = UserDetailsWithId(
    userId,
    User(userId, "password", emptyList())
  )
  SecurityContextHolder.getContext().authentication =
    UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
}

internal fun activePaymentMethod(paymentMethodId: String, userId: String): ActivePaymentMethod =
  either {
    ActivePaymentMethod.of(
      paymentMethodId = paymentMethodId,
      userId = userId,
      stripePaymentMethodId = "stripe-pm",
      lastFour = "1234",
    )
  }.fold({ error("invalid test payment method") }, { it })

internal fun failingPaymentEventPort(): PaymentEventPort = object : PaymentEventPort {
  override suspend fun <T : PaymentMethodEvent> handle(event: T): Either<WorkflowError, T> =
    error("unexpected payment method event")

  override suspend fun <T : io.liquidsoftware.base.payment.application.port.`in`.PaymentEvent> handle(event: T): Either<WorkflowError, T> =
    error("unexpected payment event")
}

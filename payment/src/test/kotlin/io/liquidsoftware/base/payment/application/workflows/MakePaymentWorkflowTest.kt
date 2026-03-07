package io.liquidsoftware.base.payment.application.workflows

import arrow.core.Either
import arrow.core.raise.either
import assertk.assertThat
import assertk.assertions.isEqualTo
import io.liquidsoftware.base.payment.PaymentMethodId
import io.liquidsoftware.base.payment.application.port.`in`.MakePaymentCommand
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMadeEvent
import io.liquidsoftware.base.payment.application.port.out.FindPaymentMethodPort
import io.liquidsoftware.base.payment.application.port.out.PaymentEventPort
import io.liquidsoftware.base.payment.application.service.StripeService
import io.liquidsoftware.base.payment.domain.ActivePaymentMethod
import io.liquidsoftware.base.payment.domain.PaymentMethod
import io.liquidsoftware.base.user.UserId
import io.liquidsoftware.common.security.ExecutionContext
import io.liquidsoftware.common.security.UserDetailsWithId
import io.liquidsoftware.common.workflow.WorkflowError
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User

class MakePaymentWorkflowTest {

  @AfterEach
  fun clearSecurityContext() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun `uses authenticated user when loading payment method and creating payment`() = runBlocking {
    authenticate("u_authenticated-user")
    val expectedPaymentMethodId = "pm_test-method"
    val recordedUserIds = mutableListOf<String>()
    var recordedPaymentEvent: PaymentMadeEvent? = null

    val workflow = MakePaymentWorkflow(
      ExecutionContext(),
      findPaymentMethodPort = object : FindPaymentMethodPort {
        override suspend fun findByPaymentMethodId(
          paymentMethodId: PaymentMethodId,
          userId: UserId,
        ): Either<WorkflowError, PaymentMethod?> {
          recordedUserIds += userId.value
          val paymentMethod = either {
            ActivePaymentMethod.of(
              paymentMethodId = paymentMethodId.value,
              userId = userId.value,
              stripePaymentMethodId = "stripe-pm",
              lastFour = "1234",
            )
          }.fold({ error("invalid test payment method") }, { it })
          return Either.Right(paymentMethod)
        }
      },
      paymentEventPort = object : PaymentEventPort {
        override suspend fun <T : io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodEvent> handle(event: T): Either<WorkflowError, T> =
          error("unexpected payment method event")

        override suspend fun <T : io.liquidsoftware.base.payment.application.port.`in`.PaymentEvent> handle(event: T): Either<WorkflowError, T> {
          recordedPaymentEvent = event as PaymentMadeEvent
          @Suppress("UNCHECKED_CAST")
          return Either.Right(event as T)
        }
      },
      stripeService = StripeService(),
    )

    val result = either {
      workflow.invoke(
        MakePaymentCommand(
          paymentMethodId = expectedPaymentMethodId,
          amount = 9000,
        )
      )
    }.fold({ error("unexpected workflow error: ${it.message}") }, { it })

    assertThat(recordedUserIds).isEqualTo(listOf("u_authenticated-user"))
    assertThat(result.paymentDto.userId).isEqualTo("u_authenticated-user")
    assertThat(recordedPaymentEvent?.paymentDto?.userId).isEqualTo("u_authenticated-user")
    assertThat(result.paymentDto.paymentMethodId).isEqualTo(expectedPaymentMethodId)
  }

  private fun authenticate(userId: String) {
    val principal = UserDetailsWithId(
      userId,
      User(userId, "password", emptyList())
    )
    SecurityContextHolder.getContext().authentication =
      UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
  }
}

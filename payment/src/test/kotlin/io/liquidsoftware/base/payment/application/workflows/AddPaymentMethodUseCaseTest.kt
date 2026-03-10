package io.liquidsoftware.base.payment.application.workflows

import arrow.core.Either
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import io.liquidsoftware.base.payment.application.port.`in`.AddPaymentMethodCommand
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodAddedEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodEvent
import io.liquidsoftware.base.payment.application.port.out.PaymentEventPort
import io.liquidsoftware.common.application.error.ApplicationError
import io.liquidsoftware.common.workflow.ServerError
import io.liquidsoftware.common.workflow.WorkflowError
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class AddPaymentMethodUseCaseTest {

  @Test
  fun `persists valid payment method event`() = runBlocking {
    var persistedEvent: PaymentMethodAddedEvent? = null
    val useCase = AddPaymentMethodUseCase(
      paymentEventPort = object : PaymentEventPort {
        override suspend fun <T : PaymentMethodEvent> handle(event: T): Either<WorkflowError, T> {
          persistedEvent = event as PaymentMethodAddedEvent
          @Suppress("UNCHECKED_CAST")
          return Either.Right(event as T)
        }

        override suspend fun <T : io.liquidsoftware.base.payment.application.port.`in`.PaymentEvent> handle(event: T): Either<WorkflowError, T> =
          error("unexpected payment event")
      },
    )

    val result = useCase.execute(
      AddPaymentMethodCommand(
        userId = "u_payment-user",
        stripePaymentMethodId = "stripe-pm",
        lastFour = "1234",
      )
    )
    val event = result.fold({ error("unexpected error: $it") }, { it })

    assertThat(event.paymentMethodDto.userId).isEqualTo("u_payment-user")
    assertThat(event.paymentMethodDto.lastFour).isEqualTo("1234")
    assertThat(persistedEvent?.paymentMethodDto?.userId).isEqualTo("u_payment-user")
  }

  @Test
  fun `maps invalid input to application validation error`() = runBlocking {
    val useCase = AddPaymentMethodUseCase(paymentEventPort = failingPaymentEventPort())

    val result = useCase.execute(
      AddPaymentMethodCommand(
        userId = "u_payment-user",
        stripePaymentMethodId = "",
        lastFour = "1234",
      )
    )

    val error = result.fold({ it }, { error("expected validation error") })
    assertThat(error).isInstanceOf(ApplicationError.Validation::class)
    Unit
  }

  @Test
  fun `maps persistence failures to application unexpected errors`() = runBlocking {
    val useCase = AddPaymentMethodUseCase(
      paymentEventPort = object : PaymentEventPort {
        override suspend fun <T : PaymentMethodEvent> handle(event: T): Either<WorkflowError, T> =
          Either.Left(ServerError("db down"))

        override suspend fun <T : io.liquidsoftware.base.payment.application.port.`in`.PaymentEvent> handle(event: T): Either<WorkflowError, T> =
          Either.Left(ServerError("db down"))
      },
    )

    val result = useCase.execute(
      AddPaymentMethodCommand(
        userId = "u_payment-user",
        stripePaymentMethodId = "stripe-pm",
        lastFour = "1234",
      )
    )

    val error = result.fold({ it }, { error("expected server error") })
    assertThat(error).isInstanceOf(ApplicationError.Unexpected::class)
    assertThat(error.message).isEqualTo("db down")
  }
}

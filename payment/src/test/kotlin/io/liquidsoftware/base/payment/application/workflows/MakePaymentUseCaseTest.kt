package io.liquidsoftware.base.payment.application.workflows

import arrow.core.Either
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import io.liquidsoftware.base.payment.PaymentMethodId
import io.liquidsoftware.base.payment.application.port.`in`.MakePaymentCommand
import io.liquidsoftware.base.payment.application.port.`in`.PaymentDeclinedError
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMadeEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodNotFoundError
import io.liquidsoftware.base.payment.application.port.out.FindPaymentMethodPort
import io.liquidsoftware.base.payment.application.port.out.PaymentEventPort
import io.liquidsoftware.base.payment.application.service.StripeService
import io.liquidsoftware.base.payment.domain.PaymentMethod
import io.liquidsoftware.base.user.UserId
import io.liquidsoftware.common.application.error.ApplicationError
import io.liquidsoftware.common.security.ExecutionContext
import io.liquidsoftware.common.workflow.ServerError
import io.liquidsoftware.common.workflow.WorkflowError
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.context.SecurityContextHolder

class MakePaymentUseCaseTest {

  @AfterEach
  fun clearSecurityContext() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun `uses authenticated user when loading payment method and creating payment`() = runBlocking {
    authenticatePaymentUser("u_authenticated-user")
    val expectedPaymentMethodId = "pm_test-method"
    val recordedUserIds = mutableListOf<String>()
    var recordedPaymentEvent: PaymentMadeEvent? = null

    val useCase = MakePaymentUseCase(
      executionContext = ExecutionContext(),
      findPaymentMethodPort = object : FindPaymentMethodPort {
        override suspend fun findByPaymentMethodId(
          paymentMethodId: PaymentMethodId,
          userId: UserId,
        ): Either<WorkflowError, PaymentMethod?> {
          recordedUserIds += userId.value
          return Either.Right(activePaymentMethod(paymentMethodId.value, userId.value))
        }
      },
      paymentEventPort = object : PaymentEventPort {
        override suspend fun <T : PaymentMethodEvent> handle(event: T): Either<WorkflowError, T> =
          error("unexpected payment method event")

        override suspend fun <T : io.liquidsoftware.base.payment.application.port.`in`.PaymentEvent> handle(event: T): Either<WorkflowError, T> {
          recordedPaymentEvent = event as PaymentMadeEvent
          @Suppress("UNCHECKED_CAST")
          return Either.Right(event as T)
        }
      },
      stripeService = StripeService(),
    )

    val result = useCase.execute(
      MakePaymentCommand(
        paymentMethodId = expectedPaymentMethodId,
        amount = 9000,
      )
    )
    val event = result.fold({ error("unexpected workflow error: ${it.message}") }, { it })

    assertThat(recordedUserIds).isEqualTo(listOf("u_authenticated-user"))
    assertThat(event.paymentDto.userId).isEqualTo("u_authenticated-user")
    assertThat(recordedPaymentEvent?.paymentDto?.userId).isEqualTo("u_authenticated-user")
    assertThat(event.paymentDto.paymentMethodId).isEqualTo(expectedPaymentMethodId)
  }

  @Test
  fun `returns payment method not found when method is missing`() = runBlocking {
    authenticatePaymentUser("u_authenticated-user")
    val useCase = MakePaymentUseCase(
      executionContext = ExecutionContext(),
      findPaymentMethodPort = object : FindPaymentMethodPort {
        override suspend fun findByPaymentMethodId(
          paymentMethodId: PaymentMethodId,
          userId: UserId,
        ): Either<WorkflowError, PaymentMethod?> = Either.Right(null)
      },
      paymentEventPort = failingPaymentEventPort(),
      stripeService = StripeService(),
    )

    val result = useCase.execute(MakePaymentCommand(paymentMethodId = "pm_missing", amount = 9000))

    val error = result.fold({ it }, { error("expected not found") })
    assertThat(error).isInstanceOf(PaymentMethodNotFoundError::class)
    assertThat(error.message).isEqualTo("pm_missing")
  }

  @Test
  fun `returns payment declined when stripe rejects charge`() = runBlocking {
    authenticatePaymentUser("u_authenticated-user")
    val useCase = MakePaymentUseCase(
      executionContext = ExecutionContext(),
      findPaymentMethodPort = object : FindPaymentMethodPort {
        override suspend fun findByPaymentMethodId(
          paymentMethodId: PaymentMethodId,
          userId: UserId,
        ): Either<WorkflowError, PaymentMethod?> = Either.Right(activePaymentMethod(paymentMethodId.value, userId.value))
      },
      paymentEventPort = failingPaymentEventPort(),
      stripeService = StripeService(),
    )

    val result = useCase.execute(MakePaymentCommand(paymentMethodId = "pm_test-method", amount = 10000))

    val error = result.fold({ it }, { error("expected payment declined") })
    assertThat(error).isInstanceOf(PaymentDeclinedError::class)
    assertThat(error.message).isEqualTo("Insufficient Funds")
  }

  @Test
  fun `maps invalid request to application validation error`() = runBlocking {
    authenticatePaymentUser("u_authenticated-user")
    val useCase = MakePaymentUseCase(
      executionContext = ExecutionContext(),
      findPaymentMethodPort = object : FindPaymentMethodPort {
        override suspend fun findByPaymentMethodId(
          paymentMethodId: PaymentMethodId,
          userId: UserId,
        ): Either<WorkflowError, PaymentMethod?> = error("unexpected payment method lookup")
      },
      paymentEventPort = failingPaymentEventPort(),
      stripeService = StripeService(),
    )

    val result = useCase.execute(MakePaymentCommand(paymentMethodId = "bad", amount = -1))

    val error = result.fold({ it }, { error("expected validation error") })
    assertThat(error).isInstanceOf(ApplicationError.Validation::class)
    Unit
  }

  @Test
  fun `maps persistence failures to application unexpected errors`() = runBlocking {
    authenticatePaymentUser("u_authenticated-user")
    val useCase = MakePaymentUseCase(
      executionContext = ExecutionContext(),
      findPaymentMethodPort = object : FindPaymentMethodPort {
        override suspend fun findByPaymentMethodId(
          paymentMethodId: PaymentMethodId,
          userId: UserId,
        ): Either<WorkflowError, PaymentMethod?> = Either.Right(activePaymentMethod(paymentMethodId.value, userId.value))
      },
      paymentEventPort = object : PaymentEventPort {
        override suspend fun <T : PaymentMethodEvent> handle(event: T): Either<WorkflowError, T> =
          error("unexpected payment method event")

        override suspend fun <T : io.liquidsoftware.base.payment.application.port.`in`.PaymentEvent> handle(event: T): Either<WorkflowError, T> =
          Either.Left(ServerError("db down"))
      },
      stripeService = StripeService(),
    )

    val result = useCase.execute(MakePaymentCommand(paymentMethodId = "pm_test-method", amount = 9000))

    val error = result.fold({ it }, { error("expected server error") })
    assertThat(error).isInstanceOf(ApplicationError.Unexpected::class)
    assertThat(error.message).isEqualTo("db down")
  }
}

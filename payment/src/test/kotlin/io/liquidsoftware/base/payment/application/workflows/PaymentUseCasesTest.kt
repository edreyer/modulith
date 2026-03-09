package io.liquidsoftware.base.payment.application.workflows

import arrow.core.Either
import arrow.core.raise.either
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import io.liquidsoftware.base.payment.PaymentMethodId
import io.liquidsoftware.base.payment.application.port.`in`.AddPaymentMethodCommand
import io.liquidsoftware.base.payment.application.port.`in`.MakePaymentCommand
import io.liquidsoftware.base.payment.application.port.`in`.PaymentDeclinedError
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMadeEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodAddedEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodNotFoundError
import io.liquidsoftware.base.payment.application.port.out.FindPaymentMethodPort
import io.liquidsoftware.base.payment.application.port.out.PaymentEventPort
import io.liquidsoftware.base.payment.application.service.StripeService
import io.liquidsoftware.base.payment.domain.ActivePaymentMethod
import io.liquidsoftware.base.payment.domain.PaymentMethod
import io.liquidsoftware.base.user.UserId
import io.liquidsoftware.common.security.ExecutionContext
import io.liquidsoftware.common.security.UserDetailsWithId
import io.liquidsoftware.common.workflow.ServerError
import io.liquidsoftware.common.workflow.WorkflowError
import io.liquidsoftware.common.workflow.WorkflowValidationError
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User

class PaymentUseCasesTest {

  @AfterEach
  fun clearSecurityContext() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun `add payment method persists valid payment method event`() = runBlocking {
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
  fun `add payment method maps invalid input to workflow validation error`() = runBlocking {
    val useCase = AddPaymentMethodUseCase(paymentEventPort = failingPaymentEventPort())

    val result = useCase.execute(
      AddPaymentMethodCommand(
        userId = "u_payment-user",
        stripePaymentMethodId = "",
        lastFour = "1234",
      )
    )

    val error = result.fold({ it }, { error("expected validation error") })
    assertThat(error).isInstanceOf(WorkflowValidationError::class)
    Unit
  }

  @Test
  fun `add payment method maps persistence failures to legacy server errors`() = runBlocking {
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
    assertThat(error).isInstanceOf(ServerError::class)
    assertThat(error.message).isEqualTo("Server Error: db down")
  }

  @Test
  fun `make payment uses authenticated user when loading payment method and creating payment`() = runBlocking {
    authenticate("u_authenticated-user")
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
  fun `make payment returns payment method not found when method is missing`() = runBlocking {
    authenticate("u_authenticated-user")
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
  fun `make payment returns payment declined when stripe rejects charge`() = runBlocking {
    authenticate("u_authenticated-user")
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
  fun `make payment maps invalid request to workflow validation error`() = runBlocking {
    authenticate("u_authenticated-user")
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
    assertThat(error).isInstanceOf(WorkflowValidationError::class)
    Unit
  }

  @Test
  fun `make payment maps persistence failures to legacy server errors`() = runBlocking {
    authenticate("u_authenticated-user")
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
    assertThat(error).isInstanceOf(ServerError::class)
    assertThat(error.message).isEqualTo("Server Error: db down")
  }

  private fun authenticate(userId: String) {
    val principal = UserDetailsWithId(
      userId,
      User(userId, "password", emptyList())
    )
    SecurityContextHolder.getContext().authentication =
      UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
  }

  private fun activePaymentMethod(paymentMethodId: String, userId: String): ActivePaymentMethod =
    either {
      ActivePaymentMethod.of(
        paymentMethodId = paymentMethodId,
        userId = userId,
        stripePaymentMethodId = "stripe-pm",
        lastFour = "1234",
      )
    }.fold({ error("invalid test payment method") }, { it })

  private fun failingPaymentEventPort(): PaymentEventPort = object : PaymentEventPort {
    override suspend fun <T : PaymentMethodEvent> handle(event: T): Either<WorkflowError, T> =
      error("unexpected payment method event")

    override suspend fun <T : io.liquidsoftware.base.payment.application.port.`in`.PaymentEvent> handle(event: T): Either<WorkflowError, T> =
      error("unexpected payment event")
  }
}

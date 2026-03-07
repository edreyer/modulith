package io.liquidsoftware.base.payment.adapter.out.persistence

import arrow.core.Either
import arrow.core.raise.either
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import io.liquidsoftware.base.payment.PaymentMethodId
import io.liquidsoftware.base.payment.application.port.`in`.PaymentDtoOut
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMadeEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodAddedEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodDtoOut
import io.liquidsoftware.base.user.UserId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.dao.DataAccessResourceFailureException
import io.liquidsoftware.common.workflow.ServerError
import java.lang.reflect.Proxy

class PaymentPersistenceAdapterArrowContractTest {

  @Test
  fun `findByPaymentMethodId returns left for invalid persisted payment method`() = runBlocking {
    val paymentMethodId = either { PaymentMethodId.of("pm_test-method") }.fold({ error("invalid test id") }, { it })
    val userId = either { UserId.of("u_test-user") }.fold({ error("invalid test id") }, { it })
    val adapter = PaymentPersistenceAdapter(
      paymentMethodRepository(
        findByPaymentMethodIdAndUserId = { _, _ ->
          PaymentMethodEntity(
            paymentMethodId = "pm_test-method",
            userId = "u_test-user",
            stripePaymentMethodId = "stripe-pm",
            lastFour = ""
          )
        }
      ),
      paymentRepository()
    )

    val result = adapter.findByPaymentMethodId(paymentMethodId, userId)

    assertThat(result is Either.Left).isTrue()
  }

  @Test
  fun `findByPaymentMethodId returns right null when payment method is missing`() = runBlocking {
    val paymentMethodId = either { PaymentMethodId.of("pm_missing") }.fold({ error("invalid test id") }, { it })
    val userId = either { UserId.of("u_test-user") }.fold({ error("invalid test id") }, { it })
    val adapter = PaymentPersistenceAdapter(
      paymentMethodRepository(findByPaymentMethodIdAndUserId = { _, _ -> null }),
      paymentRepository()
    )

    val result = adapter.findByPaymentMethodId(paymentMethodId, userId)

    assertThat(result == Either.Right(null)).isTrue()
  }

  @Test
  fun `findByPaymentMethodId returns left when repository fails`() = runBlocking {
    val paymentMethodId = either { PaymentMethodId.of("pm_test-method") }.fold({ error("invalid test id") }, { it })
    val userId = either { UserId.of("u_test-user") }.fold({ error("invalid test id") }, { it })
    val adapter = PaymentPersistenceAdapter(
      paymentMethodRepository(
        findByPaymentMethodIdAndUserId = { _, _ -> throw DataAccessResourceFailureException("db down") }
      ),
      paymentRepository()
    )

    val result = adapter.findByPaymentMethodId(paymentMethodId, userId)

    assertThat(result is Either.Left).isTrue()
    assertThat((result as Either.Left).value).isInstanceOf(ServerError::class)
    Unit
  }

  @Test
  fun `handle payment method event returns left when repository save fails`() = runBlocking {
    val adapter = PaymentPersistenceAdapter(
      paymentMethodRepository(
        findByPaymentMethodIdAndUserId = { _, _ -> null },
        save = { throw DataAccessResourceFailureException("db down") }
      ),
      paymentRepository()
    )

    val result = adapter.handle(paymentMethodAddedEvent())

    assertThat(result is Either.Left).isTrue()
    assertThat((result as Either.Left).value).isInstanceOf(ServerError::class)
    Unit
  }

  @Test
  fun `handle payment event returns left when repository save fails`() = runBlocking {
    val adapter = PaymentPersistenceAdapter(
      paymentMethodRepository(findByPaymentMethodIdAndUserId = { _, _ -> null }),
      paymentRepository(
        save = { throw DataAccessResourceFailureException("db down") }
      )
    )

    val result = adapter.handle(paymentMadeEvent())

    assertThat(result is Either.Left).isTrue()
    assertThat((result as Either.Left).value).isInstanceOf(ServerError::class)
    Unit
  }

  private fun paymentMethodAddedEvent() = PaymentMethodAddedEvent(
    paymentMethodDto = PaymentMethodDtoOut(
      paymentMethodId = "pm_test-method",
      userId = "u_test-user",
      stripePaymentMethodId = "stripe-pm",
      lastFour = "1234"
    )
  )

  private fun paymentMadeEvent() = PaymentMadeEvent(
    paymentDto = PaymentDtoOut(
      paymentId = "p_test-payment",
      paymentMethodId = "pm_test-method",
      userId = "u_test-user",
      amount = 9000
    )
  )

  private fun paymentMethodRepository(
    findByPaymentMethodIdAndUserId: (String, String) -> PaymentMethodEntity?,
    save: (PaymentMethodEntity) -> PaymentMethodEntity = { it }
  ): PaymentMethodRepository =
    Proxy.newProxyInstance(
      PaymentMethodRepository::class.java.classLoader,
      arrayOf(PaymentMethodRepository::class.java)
    ) { _, method, args ->
      when (method.name) {
        "findByPaymentMethodIdAndUserId" -> findByPaymentMethodIdAndUserId(args[0] as String, args[1] as String)
        "save" -> save(args[0] as PaymentMethodEntity)
        "toString" -> "PaymentMethodRepositoryProxy"
        "hashCode" -> System.identityHashCode(this)
        "equals" -> false
        else -> error("Unexpected repository method: ${method.name}")
      }
    } as PaymentMethodRepository

  private fun paymentRepository(
    save: (PaymentEntity) -> PaymentEntity = { it }
  ): PaymentRepository =
    Proxy.newProxyInstance(
      PaymentRepository::class.java.classLoader,
      arrayOf(PaymentRepository::class.java)
    ) { _, method, args ->
      when (method.name) {
        "findByUserId" -> null
        "save" -> save(args[0] as PaymentEntity)
        "toString" -> "PaymentRepositoryProxy"
        "hashCode" -> System.identityHashCode(this)
        "equals" -> false
        else -> error("Unexpected repository method: ${method.name}")
      }
    } as PaymentRepository
}

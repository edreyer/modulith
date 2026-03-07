package io.liquidsoftware.base.payment.adapter.out.persistence

import arrow.core.Either
import arrow.core.raise.either
import assertk.assertThat
import assertk.assertions.isTrue
import io.liquidsoftware.base.payment.PaymentMethodId
import io.liquidsoftware.base.user.UserId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
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

  private fun paymentMethodRepository(
    findByPaymentMethodIdAndUserId: (String, String) -> PaymentMethodEntity?
  ): PaymentMethodRepository =
    Proxy.newProxyInstance(
      PaymentMethodRepository::class.java.classLoader,
      arrayOf(PaymentMethodRepository::class.java)
    ) { _, method, args ->
      when (method.name) {
        "findByPaymentMethodIdAndUserId" -> findByPaymentMethodIdAndUserId(args[0] as String, args[1] as String)
        "save" -> args[0]
        "toString" -> "PaymentMethodRepositoryProxy"
        "hashCode" -> System.identityHashCode(this)
        "equals" -> false
        else -> error("Unexpected repository method: ${method.name}")
      }
    } as PaymentMethodRepository

  private fun paymentRepository(): PaymentRepository =
    Proxy.newProxyInstance(
      PaymentRepository::class.java.classLoader,
      arrayOf(PaymentRepository::class.java)
    ) { _, method, args ->
      when (method.name) {
        "findByUserId" -> null
        "save" -> args[0]
        "toString" -> "PaymentRepositoryProxy"
        "hashCode" -> System.identityHashCode(this)
        "equals" -> false
        else -> error("Unexpected repository method: ${method.name}")
      }
    } as PaymentRepository
}

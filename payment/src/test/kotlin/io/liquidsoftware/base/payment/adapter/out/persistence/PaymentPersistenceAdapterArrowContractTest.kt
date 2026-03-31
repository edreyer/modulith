package io.liquidsoftware.base.payment.adapter.out.persistence

import arrow.core.Either
import arrow.core.raise.either
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import io.liquidsoftware.base.payment.PaymentMethodId
import io.liquidsoftware.base.payment.application.port.`in`.PaymentDtoOut
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMadeEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodAddedEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodDtoOut
import io.liquidsoftware.base.user.UserId
import io.liquidsoftware.common.security.ExecutionContext
import io.liquidsoftware.common.security.UserDetailsWithId
import io.liquidsoftware.common.security.spring.SpringSecurityAccessSubjectProvider
import io.liquidsoftware.common.workflow.ServerError
import io.liquidsoftware.common.workflow.UnauthorizedWorkflowError
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import java.lang.reflect.Proxy

class PaymentPersistenceAdapterArrowContractTest {

  private fun accessSubjects() =
    SpringSecurityAccessSubjectProvider { ExecutionContext().getAccessSubject() }

  @AfterEach
  fun clearSecurityContext() {
    SecurityContextHolder.clearContext()
  }

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
      paymentRepository(),
      accessSubjects()
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
      paymentRepository(),
      accessSubjects()
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
      paymentRepository(),
      accessSubjects()
    )

    val result = adapter.findByPaymentMethodId(paymentMethodId, userId)

    assertThat(result is Either.Left).isTrue()
    assertThat((result as Either.Left).value).isInstanceOf(ServerError::class)
    Unit
  }

  @Test
  fun `handle payment method event returns left when repository save fails`() = runBlocking {
    authenticate("u_test-user")
    val adapter = PaymentPersistenceAdapter(
      paymentMethodRepository(
        findByPaymentMethodIdAndUserId = { _, _ -> null },
        save = { throw DataAccessResourceFailureException("db down") }
      ),
      paymentRepository(),
      accessSubjects()
    )

    val result = adapter.handle(paymentMethodAddedEvent())

    assertThat(result is Either.Left).isTrue()
    assertThat((result as Either.Left).value).isInstanceOf(ServerError::class)
    Unit
  }

  @Test
  fun `handle payment event returns left when repository save fails`() = runBlocking {
    authenticate("u_test-user")
    val adapter = PaymentPersistenceAdapter(
      paymentMethodRepository(findByPaymentMethodIdAndUserId = { _, _ -> null }),
      paymentRepository(
        save = { throw DataAccessResourceFailureException("db down") }
      ),
      accessSubjects()
    )

    val result = adapter.handle(paymentMadeEvent())

    assertThat(result is Either.Left).isTrue()
    assertThat((result as Either.Left).value).isInstanceOf(ServerError::class)
    Unit
  }

  @Test
  fun `findByPaymentMethodId returns left when acl check fails`() = runBlocking {
    val paymentMethodId = either { PaymentMethodId.of("pm_test-method") }.fold({ error("invalid test id") }, { it })
    val userId = either { UserId.of("u_owner") }.fold({ error("invalid test id") }, { it })
    val adapter = PaymentPersistenceAdapter(
      paymentMethodRepository(
        findByPaymentMethodIdAndUserId = { _, _ ->
          PaymentMethodEntity(
            paymentMethodId = "pm_test-method",
            userId = "u_owner",
            stripePaymentMethodId = "stripe-pm",
            lastFour = "1234"
          )
        }
      ),
      paymentRepository(),
      accessSubjects()
    )

    authenticate("u_other-user")

    val result = adapter.findByPaymentMethodId(paymentMethodId, userId)

    assertThat(result is Either.Left).isTrue()
    val error = (result as Either.Left).value
    assertThat(error).isInstanceOf(UnauthorizedWorkflowError::class)
    assertThat(error.message).isEqualTo("No access to: pm_test-method Permission: READ Subject: u_other-user")
  }

  @Test
  fun `handle payment method event returns left when acl check fails`() = runBlocking {
    val adapter = PaymentPersistenceAdapter(
      paymentMethodRepository(findByPaymentMethodIdAndUserId = { _, _ -> null }),
      paymentRepository(),
      accessSubjects()
    )

    val result = adapter.handle(paymentMethodAddedEvent(userId = "u_owner"))

    assertThat(result is Either.Left).isTrue()
    val error = (result as Either.Left).value
    assertThat(error).isInstanceOf(UnauthorizedWorkflowError::class)
    assertThat(error.message).isEqualTo("No access to: pm_test-method Permission: MANAGE Subject: u_anonymous")
  }

  @Test
  fun `handle payment event returns left when acl check fails`() = runBlocking {
    val adapter = PaymentPersistenceAdapter(
      paymentMethodRepository(findByPaymentMethodIdAndUserId = { _, _ -> null }),
      paymentRepository(),
      accessSubjects()
    )

    val result = adapter.handle(paymentMadeEvent(userId = "u_owner"))

    assertThat(result is Either.Left).isTrue()
    val error = (result as Either.Left).value
    assertThat(error).isInstanceOf(UnauthorizedWorkflowError::class)
    assertThat(error.message).isEqualTo("No access to: p_test-payment Permission: MANAGE Subject: u_anonymous")
  }

  private fun authenticate(userId: String, role: String = "ROLE_USER") {
    val principal = UserDetailsWithId(
      userId,
      User(userId, "password", listOf(SimpleGrantedAuthority(role)))
    )
    SecurityContextHolder.getContext().authentication =
      UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
  }

  private fun paymentMethodAddedEvent(userId: String = "u_test-user") = PaymentMethodAddedEvent(
    paymentMethodDto = PaymentMethodDtoOut(
      paymentMethodId = "pm_test-method",
      userId = userId,
      stripePaymentMethodId = "stripe-pm",
      lastFour = "1234"
    )
  )

  private fun paymentMadeEvent(userId: String = "u_test-user") = PaymentMadeEvent(
    paymentDto = PaymentDtoOut(
      paymentId = "p_test-payment",
      paymentMethodId = "pm_test-method",
      userId = userId,
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

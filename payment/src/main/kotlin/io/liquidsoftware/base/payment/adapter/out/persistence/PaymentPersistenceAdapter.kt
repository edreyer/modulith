package io.liquidsoftware.base.payment.adapter.out.persistence

import arrow.core.continuations.effect
import arrow.core.identity
import io.liquidsoftware.base.payment.PaymentMethodId
import io.liquidsoftware.base.payment.application.port.`in`.PaymentDtoOut
import io.liquidsoftware.base.payment.application.port.`in`.PaymentEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMadeEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodAddedEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodDtoOut
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodEvent
import io.liquidsoftware.base.payment.application.port.out.FindPaymentMethodPort
import io.liquidsoftware.base.payment.application.port.out.PaymentEventPort
import io.liquidsoftware.base.payment.domain.ActivePaymentMethod
import io.liquidsoftware.base.payment.domain.PaymentMethod
import io.liquidsoftware.base.user.UserId
import io.liquidsoftware.common.errors.ErrorHandling
import io.liquidsoftware.common.ext.className
import io.liquidsoftware.common.logging.LoggerDelegate
import io.liquidsoftware.common.security.acl.AclChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext

internal class PaymentPersistenceAdapter(
  private val paymentMethodRepository: PaymentMethodRepository,
  private val paymentRepository: PaymentRepository,
  private val ac: AclChecker
) : FindPaymentMethodPort, PaymentEventPort {

  private val log by LoggerDelegate()

  override suspend fun findByPaymentMethodId(
    paymentMethodId: PaymentMethodId, userId: UserId): PaymentMethod? =
    withContext(Dispatchers.IO) {
     paymentMethodRepository
       .findByPaymentMethodIdAndUserId(paymentMethodId.value, userId.value)
       .awaitSingleOrNull()
       ?.toPaymentMethod()
    }

  override suspend fun <T : PaymentMethodEvent> handle(event: T): T = withContext(Dispatchers.IO) {
    when (event) {
      is PaymentMethodAddedEvent -> {
        paymentMethodRepository.save(event.paymentMethodDto.toEntity()).awaitSingle()
        event
      }
      else -> throw IllegalStateException("Unknown event type: ${event.className()}")
    }
  }

  override suspend fun <T : PaymentEvent> handle(event: T): T = withContext(Dispatchers.IO) {
    when (event) {
      is PaymentMadeEvent -> {
        paymentRepository.save(event.paymentDto.toEntity()).awaitSingle()
        event
      }
      else -> throw IllegalStateException("Unknown event type: ${event.className()}")
    }
  }

  private fun PaymentMethodDtoOut.toEntity(): PaymentMethodEntity = PaymentMethodEntity(
    paymentMethodId = this.paymentMethodId,
    userId = this.userId,
    stripePaymentMethodId = this.stripePaymentMethodId,
    lastFour = this.lastFour
  )

  private fun PaymentDtoOut.toEntity(): PaymentEntity = PaymentEntity(
    paymentId = this.paymentId,
    paymentMethodId = this.paymentMethodId,
    userId = this.userId,
    amount = this.amount
  )

  private suspend fun PaymentMethodEntity.toPaymentMethod(): PaymentMethod {
    val pmEntity = this
    return effect {
      ActivePaymentMethod.of(
        pmEntity.id,
        pmEntity.userId,
        pmEntity.stripePaymentMethodId,
        pmEntity.lastFour
      )
    }
      .fold(ErrorHandling.ERROR_HANDLER, ::identity)
  }

}

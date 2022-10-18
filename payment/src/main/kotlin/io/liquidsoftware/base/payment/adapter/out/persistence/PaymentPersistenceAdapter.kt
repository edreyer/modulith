package io.liquidsoftware.base.payment.adapter.out.persistence

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
import io.liquidsoftware.common.errors.ErrorHandling.ERROR_HANDLER
import io.liquidsoftware.common.ext.className
import io.liquidsoftware.common.logging.LoggerDelegate
import io.liquidsoftware.common.security.acl.AclChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class PaymentPersistenceAdapter(
  private val paymentMethodRepository: PaymentMethodRepository,
  private val paymentRepository: PaymentRepository,
  private val ac: AclChecker
) : FindPaymentMethodPort, PaymentEventPort {

  private val logger by LoggerDelegate()

  override suspend fun findByPaymentMethodId(
    paymentMethodId: PaymentMethodId, userId: UserId): PaymentMethod? =
    withContext(Dispatchers.IO) {
     paymentMethodRepository.findByIdAndUserId(paymentMethodId.value, userId.value)?.toPaymentMethod()
    }

  override suspend fun <T : PaymentMethodEvent> handle(event: T): T = withContext(Dispatchers.IO) {
    when (event) {
      is PaymentMethodAddedEvent -> {
        paymentMethodRepository.saveAndFlush(event.paymentMethodDto.toEntity())
        event
      }
      else -> throw IllegalStateException("Unknown event type: ${event.className()}")
    }
  }



  override suspend fun <T : PaymentEvent> handle(event: T): T = withContext(Dispatchers.IO) {
    when (event) {
      is PaymentMadeEvent -> {
        paymentRepository.saveAndFlush(event.paymentDto.toEntity())
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

  private fun PaymentMethodEntity.toPaymentMethod(): PaymentMethod {
    return ActivePaymentMethod.of(
      this.id,
      this.userId,
      this.stripePaymentMethodId,
      this.lastFour
    )
      .fold(ERROR_HANDLER, ::identity)
  }

}

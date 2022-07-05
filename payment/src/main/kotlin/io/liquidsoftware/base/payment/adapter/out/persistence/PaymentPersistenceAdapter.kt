package io.liquidsoftware.base.payment.adapter.out.persistence

import io.liquidsoftware.base.payment.application.port.`in`.PaymentDtoOut
import io.liquidsoftware.base.payment.application.port.`in`.PaymentEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMadeEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodAddedEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodDtoOut
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodEvent
import io.liquidsoftware.base.payment.application.port.out.FindPaymentMethodPort
import io.liquidsoftware.base.payment.application.port.out.PaymentEventPort
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

}

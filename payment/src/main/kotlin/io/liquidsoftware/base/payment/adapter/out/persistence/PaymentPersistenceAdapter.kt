package io.liquidsoftware.base.payment.adapter.out.persistence

import arrow.core.identity
import arrow.core.raise.either
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
import io.liquidsoftware.common.ext.withContextIO
import io.liquidsoftware.common.logging.LoggerDelegate

internal class PaymentPersistenceAdapter(
  private val paymentMethodRepository: PaymentMethodRepository,
  private val paymentRepository: PaymentRepository
) : FindPaymentMethodPort, PaymentEventPort {

  private val log by LoggerDelegate()

  override suspend fun findByPaymentMethodId(
    paymentMethodId: PaymentMethodId, userId: UserId): PaymentMethod? =
    withContextIO {
     paymentMethodRepository
       .findByPaymentMethodIdAndUserId(paymentMethodId.value, userId.value)
       ?.toPaymentMethod()
    }

  override suspend fun <T : PaymentMethodEvent> handle(event: T): T = withContextIO {
    when (event) {
      is PaymentMethodAddedEvent -> {
        paymentMethodRepository.save(event.paymentMethodDto.toEntity())
        event
      }
      else -> throw IllegalStateException("Unknown event type: ${event.className()}")
    }
  }

  override suspend fun <T : PaymentEvent> handle(event: T): T = withContextIO {
    when (event) {
      is PaymentMadeEvent -> {
        paymentRepository.save(event.paymentDto.toEntity())
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
    val pmEntity = this
    return either {
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

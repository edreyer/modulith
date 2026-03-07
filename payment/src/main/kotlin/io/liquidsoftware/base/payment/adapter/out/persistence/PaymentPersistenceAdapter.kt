package io.liquidsoftware.base.payment.adapter.out.persistence

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.context.raise
import arrow.core.raise.either
import arrow.core.right
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
import io.liquidsoftware.common.ext.workflowBoundary
import io.liquidsoftware.common.ext.withContextIO
import io.liquidsoftware.common.types.ValidationErrors
import io.liquidsoftware.common.workflow.WorkflowError
import io.liquidsoftware.common.workflow.WorkflowValidationError

internal class PaymentPersistenceAdapter(
  private val paymentMethodRepository: PaymentMethodRepository,
  private val paymentRepository: PaymentRepository
) : FindPaymentMethodPort, PaymentEventPort {

  override suspend fun findByPaymentMethodId(
    paymentMethodId: PaymentMethodId,
    userId: UserId
  ): Either<WorkflowError, PaymentMethod?> =
    withContextIO {
      either {
        workflowBoundary {
          paymentMethodRepository.findByPaymentMethodIdAndUserId(paymentMethodId.value, userId.value)
        }
          ?.toPaymentMethod()
          ?.fold(
            { raise(WorkflowValidationError(it)) },
            { it }
          )
      }
    }

  override suspend fun <T : PaymentMethodEvent> handle(event: T): Either<WorkflowError, T> = withContextIO {
    either {
      when (event) {
        is PaymentMethodAddedEvent -> {
          workflowBoundary {
            paymentMethodRepository.save(event.paymentMethodDto.toEntity())
          }
          event
        }
      }
    }
  }

  override suspend fun <T : PaymentEvent> handle(event: T): Either<WorkflowError, T> = withContextIO {
    either {
      when (event) {
        is PaymentMadeEvent -> {
          workflowBoundary {
            paymentRepository.save(event.paymentDto.toEntity())
          }
          event
        }
      }
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

  private fun PaymentMethodEntity.toPaymentMethod(): Either<ValidationErrors, PaymentMethod> {
    val pmEntity = this
    return either {
      ActivePaymentMethod.of(
        pmEntity.id,
        pmEntity.userId,
        pmEntity.stripePaymentMethodId,
        pmEntity.lastFour
      )
    }
  }

}

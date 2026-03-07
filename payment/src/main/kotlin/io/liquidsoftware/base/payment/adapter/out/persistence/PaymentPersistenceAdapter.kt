package io.liquidsoftware.base.payment.adapter.out.persistence

import arrow.core.Either
import arrow.core.raise.context.raise
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
import io.liquidsoftware.common.ext.toWorkflowError
import io.liquidsoftware.common.ext.withContextIO
import io.liquidsoftware.common.ext.workflowBoundary
import io.liquidsoftware.common.security.acl.Acl
import io.liquidsoftware.common.security.acl.AclRole
import io.liquidsoftware.common.security.spring.arrow.SpringSecurityAclChecker
import io.liquidsoftware.common.types.ValidationErrors
import io.liquidsoftware.common.workflow.WorkflowError
import io.liquidsoftware.common.workflow.WorkflowValidationError

internal class PaymentPersistenceAdapter(
  private val paymentMethodRepository: PaymentMethodRepository,
  private val paymentRepository: PaymentRepository,
  private val ac: SpringSecurityAclChecker
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
            {
              ensureCanRead(it.acl())
              it
            }
          )
      }
    }

  override suspend fun <T : PaymentMethodEvent> handle(event: T): Either<WorkflowError, T> = withContextIO {
    either {
      when (event) {
        is PaymentMethodAddedEvent -> {
          ensureCanManage(event.paymentMethodDto.acl())
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
          ensureCanManage(event.paymentDto.acl())
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

  private fun PaymentMethodDtoOut.acl(): Acl = Acl.of(paymentMethodId, userId, AclRole.MANAGER)

  private fun PaymentDtoOut.acl(): Acl = Acl.of(paymentId, userId, AclRole.MANAGER)

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

  context(_: arrow.core.raise.Raise<WorkflowError>)
  private suspend fun ensureCanRead(acl: Acl) {
    either {
      ac.ensureCanRead(acl)
    }.fold(
      { raise(it.toWorkflowError()) },
      {}
    )
  }

  context(_: arrow.core.raise.Raise<WorkflowError>)
  private suspend fun ensureCanManage(acl: Acl) {
    either {
      ac.ensureCanManage(acl)
    }.fold(
      { raise(it.toWorkflowError()) },
      {}
    )
  }

}

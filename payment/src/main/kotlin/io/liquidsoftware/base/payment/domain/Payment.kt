package io.liquidsoftware.base.payment.domain

import arrow.core.raise.Raise
import io.liquidsoftware.base.payment.PaymentId
import io.liquidsoftware.base.payment.PaymentMethodId
import io.liquidsoftware.base.payment.PaymentNamespaces
import io.liquidsoftware.base.user.UserId
import io.liquidsoftware.common.persistence.NamespaceIdGenerator
import io.liquidsoftware.common.types.PositiveLong
import io.liquidsoftware.common.types.ValidationErrors

data class Payment(
  val id: PaymentId,
  val paymentMethodId: PaymentMethodId,
  val userId: UserId,
  val amount: PositiveLong

) {
  companion object {

    context(Raise<ValidationErrors>)
    fun of(id: String = NamespaceIdGenerator.nextId(PaymentNamespaces.PAYMENT_NS),
           paymentMethodId: String, userId: String, amount: Long) : Payment {
      return Payment(PaymentId.of(id),
        PaymentMethodId.of(paymentMethodId),
        UserId.of(userId),
        PositiveLong.of(amount)
      )
    }
  }
}

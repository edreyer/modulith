package io.liquidsoftware.base.payment.domain

import arrow.core.zip
import io.liquidsoftware.base.payment.PaymentId
import io.liquidsoftware.base.payment.PaymentMethodId
import io.liquidsoftware.base.payment.PaymentNamespaces
import io.liquidsoftware.base.user.UserId
import io.liquidsoftware.common.persistence.NamespaceIdGenerator
import io.liquidsoftware.common.types.PositiveLong
import io.liquidsoftware.common.types.ValidationErrorNel

data class Payment(
  val id: PaymentId,
  val paymentMethodId: PaymentMethodId,
  val userId: UserId,
  val amount: PositiveLong

) {
  companion object {

    fun of(id: String = NamespaceIdGenerator.nextId(PaymentNamespaces.PAYMENT_NS),
           paymentMethodId: String, userId: String, amount: Long) : ValidationErrorNel<Payment> {
      return PaymentId.of(id).zip(
        PaymentMethodId.of(paymentMethodId),
        UserId.of(userId),
        PositiveLong.of(amount)
      ) { pId, pmId, uId, a ->
        Payment(pId, pmId, uId, a)
      }
    }
  }
}

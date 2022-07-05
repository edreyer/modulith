package io.liquidsoftware.base.payment.domain

import arrow.core.zip
import io.liquidsoftware.base.payment.PaymentMethodId
import io.liquidsoftware.base.payment.PaymentNamespaces
import io.liquidsoftware.base.user.UserId
import io.liquidsoftware.common.persistence.NamespaceIdGenerator
import io.liquidsoftware.common.security.acl.Acl
import io.liquidsoftware.common.security.acl.AclRole
import io.liquidsoftware.common.types.NonEmptyString
import io.liquidsoftware.common.types.ValidationErrorNel

internal interface PaymentMethodFields {
  val id: PaymentMethodId
  val userId: UserId
  val stripePaymentMethodId: NonEmptyString
  val lastFour: NonEmptyString
}

internal data class PaymentMethodData(
  override val id: PaymentMethodId,
  override val userId: UserId,
  override val stripePaymentMethodId: NonEmptyString,
  override val lastFour: NonEmptyString
) : PaymentMethodFields

internal sealed class PaymentMethod : PaymentMethodFields {
  fun acl() = Acl.of(id.value, userId.value, AclRole.MANAGER)
}

internal data class ActivePaymentMethod(
  val data: PaymentMethodData
) : PaymentMethod(), PaymentMethodFields by data {
  companion object {
    fun of(paymentMethodId: String = NamespaceIdGenerator.nextId(PaymentNamespaces.PAYMENT_METHOD_NS),
           userId: String, stripePaymentMethodId: String, lastFour: String) : ValidationErrorNel<ActivePaymentMethod> {
      return PaymentMethodId.of(paymentMethodId).zip(
        UserId.of(userId),
        NonEmptyString.of(stripePaymentMethodId),
        NonEmptyString.of(lastFour)
      ) { pmId, uId, spmId, lf ->
        ActivePaymentMethod(PaymentMethodData(pmId, uId, spmId, lf))
      }
    }
  }
}

// TODO: Define Expired/Deleted PM types

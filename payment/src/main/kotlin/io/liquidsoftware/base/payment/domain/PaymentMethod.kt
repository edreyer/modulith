package io.liquidsoftware.base.payment.domain

import arrow.core.continuations.EffectScope
import io.liquidsoftware.base.payment.PaymentMethodId
import io.liquidsoftware.base.payment.PaymentNamespaces
import io.liquidsoftware.base.user.UserId
import io.liquidsoftware.common.persistence.NamespaceIdGenerator
import io.liquidsoftware.common.security.acl.Acl
import io.liquidsoftware.common.security.acl.AclRole
import io.liquidsoftware.common.types.NonEmptyString
import io.liquidsoftware.common.types.ValidationErrors

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
    context(EffectScope<ValidationErrors>)
    suspend fun of(paymentMethodId: String = NamespaceIdGenerator.nextId(PaymentNamespaces.PAYMENT_METHOD_NS),
           userId: String, stripePaymentMethodId: String, lastFour: String) : ActivePaymentMethod {
      return ActivePaymentMethod(PaymentMethodData(
        PaymentMethodId.of(paymentMethodId),
        UserId.of(userId),
        NonEmptyString.of(stripePaymentMethodId),
        NonEmptyString.of(lastFour)
      ))
    }
  }
}

// TODO: Define Expired/Deleted PM types

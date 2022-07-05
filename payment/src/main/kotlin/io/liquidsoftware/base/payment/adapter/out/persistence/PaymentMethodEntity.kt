package io.liquidsoftware.base.payment.adapter.out.persistence

import io.liquidsoftware.base.payment.PaymentNamespaces
import io.liquidsoftware.common.persistence.BaseEntity
import io.liquidsoftware.common.persistence.NamespaceIdGenerator
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "payment_methods")
internal class PaymentMethodEntity(

  paymentMethodId: String = NamespaceIdGenerator.nextId(PaymentNamespaces.PAYMENT_METHOD_NS),

  var userId: String,
  var stripePaymentMethodId: String,
  var lastFour: String

) : BaseEntity(paymentMethodId, PaymentNamespaces.PAYMENT_METHOD_NS)

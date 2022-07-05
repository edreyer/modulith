package io.liquidsoftware.base.payment.adapter.out.persistence

import io.liquidsoftware.base.payment.PaymentNamespaces
import io.liquidsoftware.common.persistence.BaseEntity
import io.liquidsoftware.common.persistence.NamespaceIdGenerator
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "payments")
internal class PaymentEntity(

  paymentId: String = NamespaceIdGenerator.nextId(PaymentNamespaces.PAYMENT_NS),

  var userId: String,
  var paymentMethodId: String,
  var amount: Int

) : BaseEntity(paymentId, PaymentNamespaces.PAYMENT_NS)

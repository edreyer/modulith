package io.liquidsoftware.base.payment.adapter.out.persistence

import io.liquidsoftware.base.payment.PaymentNamespaces
import io.liquidsoftware.common.persistence.BaseMongoEntity
import io.liquidsoftware.common.persistence.NamespaceIdGenerator
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document("payment_methods")
internal class PaymentMethodEntity(

  @Indexed(unique = true)
  var paymentMethodId: String = NamespaceIdGenerator.nextId(PaymentNamespaces.PAYMENT_METHOD_NS),
  @Indexed
  var userId: String,
  @Indexed
  var stripePaymentMethodId: String,

  var lastFour: String

) : BaseMongoEntity(paymentMethodId, PaymentNamespaces.PAYMENT_METHOD_NS)

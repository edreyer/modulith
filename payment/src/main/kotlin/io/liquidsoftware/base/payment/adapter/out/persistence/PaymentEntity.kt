package io.liquidsoftware.base.payment.adapter.out.persistence

import io.liquidsoftware.base.payment.PaymentNamespaces
import io.liquidsoftware.common.persistence.BaseMongoEntity
import io.liquidsoftware.common.persistence.NamespaceIdGenerator
import io.liquidsoftware.common.security.acl.Acl
import io.liquidsoftware.common.security.acl.AclRole
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document("payments")
internal class PaymentEntity(

  @Indexed(unique = true)
  var paymentId: String = NamespaceIdGenerator.nextId(PaymentNamespaces.PAYMENT_NS),
  @Indexed
  var userId: String,
  @Indexed
  var paymentMethodId: String,

  var amount: Long

) : BaseMongoEntity(paymentId, PaymentNamespaces.PAYMENT_NS) {

  fun acl() = Acl.of(paymentId, userId, AclRole.MANAGER)

}

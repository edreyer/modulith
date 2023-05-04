package io.liquidsoftware.base.payment

import arrow.core.raise.Raise
import io.liquidsoftware.base.payment.PaymentNamespaces.PAYMENT_METHOD_NS
import io.liquidsoftware.base.payment.PaymentNamespaces.PAYMENT_NS
import io.liquidsoftware.common.persistence.NamespaceIdGenerator
import io.liquidsoftware.common.types.SimpleType
import io.liquidsoftware.common.types.ValidationErrors
import io.liquidsoftware.common.types.ensure
import org.valiktor.functions.matches
import org.valiktor.validate

object PaymentNamespaces {
  const val PAYMENT_NS = "p_"
  const val PAYMENT_METHOD_NS = "pm_"
}

class PaymentId private constructor(override val value: String)
  : SimpleType<String>() {
  companion object {
    context(Raise<ValidationErrors>)
    fun of(value: String): PaymentId = ensure {
      validate(PaymentId(value)) {
        validate(PaymentId::value).matches("${PAYMENT_NS}.*".toRegex())
      }
    }.bind()

    fun create() = PaymentId(NamespaceIdGenerator.nextId(PAYMENT_NS))
  }
}

class PaymentMethodId private constructor(override val value: String) : SimpleType<String>() {
  companion object {
    context(Raise<ValidationErrors>)
    fun of(value: String): PaymentMethodId = ensure {
      validate(PaymentMethodId(value)) {
        validate(PaymentMethodId::value).matches("${PAYMENT_METHOD_NS}.*".toRegex())
      }
    }.bind()

    fun create() = PaymentMethodId(NamespaceIdGenerator.nextId(PAYMENT_METHOD_NS))
  }

}

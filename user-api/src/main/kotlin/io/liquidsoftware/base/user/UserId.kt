package io.liquidsoftware.base.user

import arrow.core.continuations.EffectScope
import io.liquidsoftware.base.user.UserNamespace.NAMESPACE
import io.liquidsoftware.common.persistence.NamespaceIdGenerator
import io.liquidsoftware.common.types.SimpleType
import io.liquidsoftware.common.types.ValidationErrors
import io.liquidsoftware.common.types.ensure
import org.valiktor.functions.matches
import org.valiktor.validate

object UserNamespace {
  const val NAMESPACE = "u_"
}

class UserId private constructor(override val value: String)
  : SimpleType<String>() {
  companion object {
    context(EffectScope<ValidationErrors>)
    suspend fun of(value: String): UserId = ensure {
      validate(UserId(value)) {
        validate(UserId::value).matches("$NAMESPACE.*".toRegex())
      }
    }.bind()

    context(EffectScope<ValidationErrors>)
    suspend fun create() = of(NamespaceIdGenerator.nextId(NAMESPACE))
  }
}

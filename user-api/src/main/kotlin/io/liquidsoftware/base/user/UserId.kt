package io.liquidsoftware.base.user

import org.valiktor.functions.matches
import org.valiktor.validate
import io.liquidsoftware.base.user.UserNamespace.NAMESPACE
import io.liquidsoftware.common.persistence.NamespaceIdGenerator
import io.liquidsoftware.common.types.SimpleType
import io.liquidsoftware.common.types.ValidationErrorNel
import io.liquidsoftware.common.types.ensure

object UserNamespace {
  const val NAMESPACE = "u_"
}

class UserId private constructor(override val value: String)
  : SimpleType<String>() {
  companion object {
    fun of(value: String): ValidationErrorNel<UserId> = ensure {
      validate(UserId(value)) {
        validate(UserId::value).matches("$NAMESPACE.*".toRegex())
      }
    }
    fun create() = UserId(NamespaceIdGenerator.nextId(NAMESPACE))
  }
}

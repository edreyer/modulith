package ventures.dvx.base.user

import org.valiktor.functions.matches
import org.valiktor.validate
import ventures.dvx.base.user.UserNamespace.NAMESPACE
import ventures.dvx.common.persistence.NamespaceIdGenerator
import ventures.dvx.common.types.SimpleType
import ventures.dvx.common.types.ValidationErrorNel
import ventures.dvx.common.types.ensure

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

package ventures.dvx.base.user

import org.valiktor.functions.matches
import org.valiktor.validate
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
        validate(UserId::value).matches("${UserNamespace.NAMESPACE}.*".toRegex())
      }
    }
  }
}

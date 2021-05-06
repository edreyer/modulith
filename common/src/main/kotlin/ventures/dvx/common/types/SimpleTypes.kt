package ventures.dvx.common.types

import arrow.core.ValidatedNel
import arrow.core.invalidNel
import arrow.core.validNel
import org.valiktor.ConstraintViolationException
import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.matches
import org.valiktor.i18n.mapToMessage
import org.valiktor.validate

data class ValidationError(val error: String)

inline class NonEmptyString private constructor(val value: String) {
  companion object {
    fun of(value: String): ValidatedNel<ValidationError, NonEmptyString> = ensure {
      validate(NonEmptyString(value)) {
        validate(NonEmptyString::value).isNotEmpty()
      }
    }
  }
}

inline class EmailAddress private constructor(val value: String) {
  companion object {
    fun of(value: String): ValidatedNel<ValidationError, EmailAddress> = ensure {
      validate(EmailAddress(value)) {
        validate(EmailAddress::value).isNotEmpty()
        validate(EmailAddress::value).isEmail()
      }
    }
  }
}

inline class PostalCode private constructor(val value: String) {
  companion object {
    fun of(value: String): ValidatedNel<ValidationError, PostalCode> = ensure {
      validate(PostalCode(value)) {
        validate(PostalCode::value).matches("""\d{5}""".toRegex())
      }
    }
  }
}

inline class Msisdn private constructor(val value: String) {
  companion object {
    fun of(value: String): ValidatedNel<ValidationError, Msisdn> = ensure {
      validate(Msisdn(value)) {
        validate(Msisdn::value).matches("""\+1\d{10}""".toRegex())
      }
    }
  }
}

internal inline fun <reified T> ensure(ensureFn: () -> T): ValidatedNel<ValidationError, T> = try {
  ensureFn().validNel()
} catch (ex: ConstraintViolationException) {
  ex
    .constraintViolations
    .mapToMessage()
    .joinToString("\n") { "\t\"${it.value}\" of ${T::class.simpleName}.${it.property}: ${it.message}" }
    .let {
      ValidationError(it).invalidNel()
    }
}

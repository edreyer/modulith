package ventures.dvx.common.types

import arrow.core.NonEmptyList
import arrow.core.ValidatedNel
import arrow.core.invalid
import arrow.core.validNel
import org.valiktor.ConstraintViolationException
import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isValid
import org.valiktor.functions.matches
import org.valiktor.i18n.mapToMessage
import org.valiktor.validate
import ventures.dvx.common.validation.MsisdnParser

@JvmInline
value class ValidationError(val error: String)

// Helpful extension functions

fun NonEmptyList<ValidationError>.toErrStrings() =
  this.map { it.toString() }.toList()
fun NonEmptyList<ValidationError>.toErrString() =
  this.map { it.toString() }.joinToString { "$it, " }

// Returns the Validated value OR throws
fun <T:SimpleType<*>> ValidatedNel<ValidationError, T>.getOrThrow(): T = this.fold(
  { throw IllegalStateException("Validation Error: ${it.toErrString()}") },
  { it }
)

// Can be used as shortcuts to create simple types from Strings
// Note that these throw,
fun String.toNonEmptyString() = NonEmptyString.of(this).getOrThrow()
fun String.toEmailAddress() = EmailAddress.of(this).getOrThrow()
fun String.toMsisdn() = Msisdn.of(this).getOrThrow()
fun String.toPostalCode() = PostalCode.of(this).getOrThrow()

abstract class SimpleType<T> {
  abstract val value: T
  override fun toString(): String = value.toString()
}

class NonEmptyString private constructor(override val value: String)
  : SimpleType<String>() {
  companion object {
    fun of(value: String): ValidatedNel<ValidationError, NonEmptyString> = ensure {
      validate(NonEmptyString(value)) {
        validate(NonEmptyString::value).isNotEmpty()
      }
    }
  }
}

class EmailAddress private constructor(override val value: String): SimpleType<String>() {
  companion object {
    fun of(value: String): ValidatedNel<ValidationError, EmailAddress> = ensure {
      validate(EmailAddress(value)) {
        validate(EmailAddress::value).isNotEmpty()
        validate(EmailAddress::value).isEmail()
      }
    }
  }
}

class PostalCode private constructor(override val value: String): SimpleType<String>() {
  companion object {
    fun of(value: String): ValidatedNel<ValidationError, PostalCode> = ensure {
      validate(PostalCode(value)) {
        validate(PostalCode::value).matches("""\d{5}""".toRegex())
      }
    }
  }
}

class Msisdn private constructor(override val value: String): SimpleType<String>() {
  companion object {
    private val msisdnParser = MsisdnParser()
    fun of(value: String): ValidatedNel<ValidationError, Msisdn> = ensure {
      validate(Msisdn(value)) {
        validate(Msisdn::value).isValid { msisdnParser.isValid(it) }
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
    .map { "'${it.value}' of ${T::class.simpleName}.${it.property}: ${it.message}" }
    .map { ValidationError(it) }
    .let { NonEmptyList.fromListUnsafe(it).invalid() }
}

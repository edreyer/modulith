package io.liquidsoftware.common.types

import arrow.core.Nel
import arrow.core.ValidatedNel
import arrow.core.continuations.EffectScope
import arrow.core.invalid
import arrow.core.toNonEmptyListOrNull
import arrow.core.validNel
import io.liquidsoftware.common.validation.MsisdnParser
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import org.valiktor.ConstraintViolationException
import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isPositiveOrZero
import org.valiktor.functions.isValid
import org.valiktor.functions.matches
import org.valiktor.i18n.mapToMessage
import org.valiktor.validate

@JvmInline
value class ValidationError(val error: String)

@ResponseStatus(code = HttpStatus.PRECONDITION_FAILED)
data class ValidationException(val errors: Nel<ValidationError>)
  : RuntimeException(errors.toErrString())

typealias ValidationErrorNel<T> = ValidatedNel<ValidationError, T>
typealias ValidationErrors = Nel<ValidationError>

// Helpful extension functions

fun ValidationErrors.toErrStrings() =
  this.map { it.error }.toList()
fun ValidationErrors.toErrString() =
  this.map { it.error }.joinToString { "$it\n" }

// Returns the Validated value OR throws
context(EffectScope<Throwable>)
suspend fun <T:SimpleType<*>> ValidationErrorNel<T>.getOrShift(): T = this.fold(
  { shift(ValidationException(it)) },
  { it }
)

// Can be used as shortcuts to create simple types from Strings
// Note that these throw,
context(EffectScope<ValidationErrors>)
suspend fun String.toNonEmptyString() = NonEmptyString.of(this)
context(EffectScope<ValidationErrors>)
suspend fun String.toEmailAddress() = EmailAddress.of(this)
context(EffectScope<ValidationErrors>)
suspend fun String.toMsisdn() = Msisdn.of(this)
context(EffectScope<ValidationErrors>)
suspend fun String.toPostalCode() = PostalCode.of(this)

abstract class SimpleType<T> {
  abstract val value: T
  override fun toString(): String = value.toString()
}

class NonEmptyString private constructor(override val value: String)
  : SimpleType<String>() {
  companion object {
    context(EffectScope<ValidationErrors>)
    suspend fun of(value: String): NonEmptyString = ensure {
      validate(NonEmptyString(value)) {
        validate(NonEmptyString::value).isNotEmpty()
      }
    }.bind()
  }
}

class PositiveInt private constructor(override val value: Int)
  : SimpleType<Int>() {
  companion object {
    context(EffectScope<ValidationErrors>)
    suspend fun of(value: Int): PositiveInt = ensure {
      validate(PositiveInt(value)) {
        validate(PositiveInt::value).isPositiveOrZero()
      }
    }.bind()
  }
}

class PositiveLong private constructor(override val value: Long)
  : SimpleType<Long>() {
  companion object {
    context(EffectScope<ValidationErrors>)
    suspend fun of(value: Long): PositiveLong = ensure {
      validate(PositiveLong(value)) {
        validate(PositiveLong::value).isPositiveOrZero()
      }
    }.bind()
  }
}

class EmailAddress private constructor(override val value: String): SimpleType<String>() {
  companion object {
    context(EffectScope<ValidationErrors>)
    suspend fun of(value: String): EmailAddress = ensure {
      validate(EmailAddress(value)) {
        validate(EmailAddress::value).isNotEmpty()
        validate(EmailAddress::value).isEmail()
      }
    }.bind()
  }
}

class PostalCode private constructor(override val value: String): SimpleType<String>() {
  companion object {
    context(EffectScope<ValidationErrors>)
    suspend fun of(value: String): PostalCode = ensure {
      validate(PostalCode(value)) {
        validate(PostalCode::value).matches("""\d{5}""".toRegex())
      }
    }.bind()
  }
}

class Msisdn private constructor(override val value: String): SimpleType<String>() {
  companion object {
    context(EffectScope<ValidationErrors>)
    suspend fun of(value: String): Msisdn = ensure {
      val msisdn = validate(Msisdn(value)) {
        validate(Msisdn::value).isValid { MsisdnParser.isValid(it) }
      }
      Msisdn(MsisdnParser.toInternational(msisdn.value))
    }.bind()
  }
}

inline fun <reified T> ensure(ensureFn: () -> T): ValidationErrorNel<T> = try {
  ensureFn().validNel()
} catch (ex: ConstraintViolationException) {
  ex
    .constraintViolations
    .mapToMessage()
    .map { "'${it.value}' of ${T::class.simpleName}.${it.property}: ${it.message}" }
    .map { ValidationError(it) }
    .let { it.toNonEmptyListOrNull()!! }
    .invalid()
}

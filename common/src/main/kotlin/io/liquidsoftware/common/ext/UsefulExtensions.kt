package io.liquidsoftware.common.ext

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.Validated
import arrow.core.continuations.ResultEffectScope
import io.liquidsoftware.common.types.ValidationErrorNel
import io.liquidsoftware.common.types.ValidationException
import org.springframework.web.bind.annotation.ResponseStatus
import java.util.Optional

/* Get the name of any class */
fun Any.className(): String = this::class.qualifiedName ?: this::class.java.name

/* Convert a Validated into a Result */
inline fun <reified T> ValidationErrorNel<T>.toResult(): Result<T> = when (this) {
  is Validated.Valid -> Result.success(this.value)
  is Validated.Invalid -> Result.failure(ValidationException(this.value))
}

context(ResultEffectScope)
suspend inline fun <reified T> ValidationErrorNel<T>.unwrap(): T = when (this) {
  is Validated.Valid -> this.value
  is Validated.Invalid -> shift(ValidationException(this.value))
}

fun Throwable.hasResponseStatus(): Boolean = this.javaClass.isAnnotationPresent(ResponseStatus::class.java)
fun RuntimeException.hasResponseStatus(): Boolean = this.javaClass.isAnnotationPresent(ResponseStatus::class.java)

fun <T> Optional<T>.unwrap(): T? = orElse(null)

inline fun <T> Result<T>.mapError(f: (ex: Throwable) -> Throwable): Result<T> =
  if (this.isSuccess) {
    this
  } else {
    Result.failure(f(this.exceptionOrNull() ?: IllegalStateException("Unknown Error")))
  }

fun <E: Throwable, T> Either<E, T>.toResult() = when (this) {
  is Left -> Result.failure(this.value)
  is Right -> Result.success(this.value)
}

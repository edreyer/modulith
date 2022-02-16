package io.liquidsoftware.common.ext

import arrow.core.Validated
import org.springframework.web.bind.annotation.ResponseStatus
import io.liquidsoftware.common.types.ValidationErrorNel
import io.liquidsoftware.common.types.ValidationException

/* Get the name of any class */
fun Any.className(): String = this::class.qualifiedName ?: this::class.java.name

/* Convert a Validated into a Result */
inline fun <reified T> ValidationErrorNel<T>.toResult(): Result<T> = when (this) {
  is Validated.Valid -> Result.success(this.value)
  is Validated.Invalid -> Result.failure(ValidationException(this.value))
}

fun Throwable.hasResponseStatus(): Boolean = this.javaClass.isAnnotationPresent(ResponseStatus::class.java)
fun RuntimeException.hasResponseStatus(): Boolean = this.javaClass.isAnnotationPresent(ResponseStatus::class.java)

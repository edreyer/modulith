package ventures.dvx.common.ext

import arrow.core.Validated
import arrow.core.ValidatedNel
import ventures.dvx.common.types.ValidationError
import ventures.dvx.common.types.ValidationException

/* Get the name of any class */
fun Any.className(): String = this::class.qualifiedName ?: this::class.java.name

/* Convert a Validated into a Result */
inline fun <reified T> ValidatedNel<ValidationError, T>.toResult(): Result<T> = when (this) {
  is Validated.Valid -> Result.success(this.value)
  is Validated.Invalid -> Result.failure(ValidationException(this.value))
}
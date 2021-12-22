package ventures.dvx.common.ext

import arrow.core.Validated
import org.springframework.web.bind.annotation.ResponseStatus
import ventures.dvx.common.types.ValidationErrorNel
import ventures.dvx.common.types.ValidationException

/* Get the name of any class */
fun Any.className(): String = this::class.qualifiedName ?: this::class.java.name

/* Convert a Validated into a Result */
inline fun <reified T> ValidationErrorNel<T>.toResult(): Result<T> = when (this) {
  is Validated.Valid -> Result.success(this.value)
  is Validated.Invalid -> Result.failure(ValidationException(this.value))
}

fun RuntimeException.hasResponseStatus(): Boolean = this.javaClass.isAnnotationPresent(ResponseStatus::class.java)

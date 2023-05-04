package io.liquidsoftware.common.ext

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.raise.Effect
import arrow.core.raise.Raise
import arrow.core.raise.toEither
import io.liquidsoftware.common.types.ValidationErrorNel
import io.liquidsoftware.common.workflow.ServerError
import io.liquidsoftware.common.workflow.WorkflowError
import io.liquidsoftware.common.workflow.WorkflowValidationError
import org.springframework.web.bind.annotation.ResponseStatus
import java.util.Optional

/* Get the name of any class */
fun Any.className(): String = this::class.qualifiedName ?: this::class.java.name

/* Convert a Validated into a Result */
//inline fun <reified T> ValidationErrorNel<T>.toResult(): Result<T> = when (this) {
//  is Validated.Valid -> Result.success(this.value)
//  is Validated.Invalid -> Result.failure(ValidationException(this.value))
//}

context(Raise<WorkflowError>)
inline fun <reified T> ValidationErrorNel<T>.getOrRaise(): T = when (this) {
  is Right -> this.value
  is Left -> raise(WorkflowValidationError(this.value))
}

//context(Raise<WorkflowError>)
//inline fun <reified E, reified T> Either<E, T>.getOrRaise(): T = when (this) {
//  is Right -> this.value
//  is Left -> raise(WorkflowValidationError(this.value))
//}

context(Raise<WorkflowError>)
inline fun <reified T> Result<T>.getOrRaise(): T = this.getOrElse {
  raise(ServerError(it.message ?: ""))
}

//context(Raise<WorkflowError>)
//suspend inline fun <reified T> Effect<ValidationErrors, T>.getOrShift(): T =
//  this.handleError {
//    raise(WorkflowValidationError(it))
//  }.bind()

fun Throwable.hasResponseStatus(): Boolean = this.javaClass.isAnnotationPresent(ResponseStatus::class.java)

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

suspend fun <E: Throwable, T> Effect<E, T>.toResult() = this.toEither().toResult()

package io.liquidsoftware.common.web

import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler


@ControllerAdvice
class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException::class)
  protected fun handleMethodArgumentNotValid(ex: MethodArgumentNotValidException): Map<String, String> {
    val errors: MutableMap<String, String> = HashMap()
    ex.bindingResult.allErrors.forEach { error ->
      val fieldName = (error as FieldError).field
      val errorMessage = error.getDefaultMessage()
      errors[fieldName] = errorMessage ?: "N/A"
    }
    return errors
  }

}

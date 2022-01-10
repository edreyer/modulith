package io.liquidsoftware.common.validation

import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class CustomValidator : ConstraintValidator<CustomValidated, Any> {
  override fun initialize(constraintAnnotation: CustomValidated) {}
  override fun isValid(value: Any, context: ConstraintValidatorContext): Boolean {
    if (value !is Boolean) throw IllegalArgumentException("Expected boolean value: ${value}")
    return value
  }
}

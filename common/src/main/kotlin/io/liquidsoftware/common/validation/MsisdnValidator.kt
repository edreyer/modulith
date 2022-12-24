package io.liquidsoftware.common.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class MsisdnValidator : ConstraintValidator<Msisdn, String> {

  override fun initialize(constraintAnnotation: Msisdn) {}

  override fun isValid(value: String, context: ConstraintValidatorContext): Boolean = try {
    MsisdnParser.isValid(value)
  } catch (e: RuntimeException) {
    false
  }

}

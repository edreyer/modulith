package ventures.dvx.common.validation

import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class MsisdnValidator : ConstraintValidator<Msisdn, String> {

  override fun initialize(constraintAnnotation: Msisdn) {}

  override fun isValid(value: String, context: ConstraintValidatorContext): Boolean = try {
    MsisdnParser.isValid(value)
  } catch (e: RuntimeException) {
    false
  }

}

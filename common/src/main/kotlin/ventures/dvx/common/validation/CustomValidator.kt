package ventures.dvx.common.validation

import com.google.common.base.Preconditions
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class CustomValidator : ConstraintValidator<CustomValidated, Any> {
  override fun initialize(constraintAnnotation: CustomValidated) {}
  override fun isValid(value: Any, context: ConstraintValidatorContext): Boolean {
    Preconditions.checkArgument(value is Boolean, "Expected boolean value: %s", value)
    return value as Boolean
  }
}

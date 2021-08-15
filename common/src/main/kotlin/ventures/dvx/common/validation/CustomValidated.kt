package ventures.dvx.common.validation

import javax.validation.Constraint
import javax.validation.Payload
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER
import kotlin.reflect.KClass

@MustBeDocumented
@Retention(RUNTIME)
@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
@Constraint(validatedBy = [CustomValidator::class])
annotation class CustomValidated(
  val message: String = "{ Custom validation failure }",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = []
)


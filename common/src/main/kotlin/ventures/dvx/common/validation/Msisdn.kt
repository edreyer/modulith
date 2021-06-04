package ventures.dvx.common.validation

import javax.validation.Constraint
import javax.validation.Payload
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER
import kotlin.reflect.KClass

@MustBeDocumented
@Retention(RUNTIME)
@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, FIELD)
@Constraint(validatedBy = [MsisdnValidator::class])
annotation class Msisdn(
  val message: String = "{Invalid Mobile Number please enter a valid one }",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = []
)

package io.liquidsoftware.common.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.*
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


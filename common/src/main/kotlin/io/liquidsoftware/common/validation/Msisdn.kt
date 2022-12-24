package io.liquidsoftware.common.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.*
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

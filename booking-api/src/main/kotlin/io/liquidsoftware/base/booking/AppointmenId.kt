package io.liquidsoftware.base.booking

import io.liquidsoftware.base.booking.BookingNamespaces.APPOINTMENT_NS
import io.liquidsoftware.common.persistence.NamespaceIdGenerator
import io.liquidsoftware.common.types.SimpleType
import io.liquidsoftware.common.types.ValidationErrorNel
import io.liquidsoftware.common.types.ensure
import org.valiktor.functions.matches
import org.valiktor.validate

object BookingNamespaces {
  const val APPOINTMENT_NS = "a_"
}

class AppointmentId private constructor(override val value: String)
  : SimpleType<String>() {
  companion object {
    fun of(value: String): ValidationErrorNel<AppointmentId> = ensure {
      validate(AppointmentId(value)) {
        validate(AppointmentId::value).matches("${APPOINTMENT_NS}.*".toRegex())
      }
    }
    fun create() = AppointmentId(NamespaceIdGenerator.nextId(APPOINTMENT_NS))
  }
}

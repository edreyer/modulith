package ventures.dvx.base.booking

import org.valiktor.functions.matches
import org.valiktor.validate
import ventures.dvx.base.booking.BookingNamespaces.APPOINTMENT_NS
import ventures.dvx.common.persistence.NamespaceIdGenerator
import ventures.dvx.common.types.SimpleType
import ventures.dvx.common.types.ValidationErrorNel
import ventures.dvx.common.types.ensure

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

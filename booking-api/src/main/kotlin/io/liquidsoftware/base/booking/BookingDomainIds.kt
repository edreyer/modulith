package io.liquidsoftware.base.booking

import arrow.core.raise.Raise
import io.liquidsoftware.base.booking.BookingNamespaces.APPOINTMENT_NS
import io.liquidsoftware.base.booking.BookingNamespaces.WORK_WORDER_NS
import io.liquidsoftware.common.ext.bind
import io.liquidsoftware.common.persistence.NamespaceIdGenerator
import io.liquidsoftware.common.types.SimpleType
import io.liquidsoftware.common.types.ValidationErrors
import io.liquidsoftware.common.types.ensure
import org.valiktor.functions.matches
import org.valiktor.validate

object BookingNamespaces {
  const val APPOINTMENT_NS = "a_"
  const val WORK_WORDER_NS = "wo_"
}

class AppointmentId private constructor(override val value: String)
  : SimpleType<String>() {
  companion object {
    context(_: Raise<ValidationErrors>)
    fun of(value: String): AppointmentId = ensure {
      validate(AppointmentId(value)) {
        validate(AppointmentId::value).matches("${APPOINTMENT_NS}.*".toRegex())
      }
    }.bind()
    fun create() = AppointmentId(NamespaceIdGenerator.nextId(APPOINTMENT_NS))
  }
}

class WorkOrderId private constructor(override val value: String)
  : SimpleType<String>() {
  companion object {
    context(_: Raise<ValidationErrors>)
    fun of(value: String): WorkOrderId = ensure {
      validate(WorkOrderId(value)) {
        validate(WorkOrderId::value).matches("${WORK_WORDER_NS}.*".toRegex())
      }
    }.bind()
    fun create() = WorkOrderId(NamespaceIdGenerator.nextId(WORK_WORDER_NS))
  }
}

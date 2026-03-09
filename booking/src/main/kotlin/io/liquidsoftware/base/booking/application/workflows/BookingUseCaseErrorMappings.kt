package io.liquidsoftware.base.booking.application.workflows

import io.liquidsoftware.base.booking.application.port.`in`.AppointmentNotFoundError
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentValidationError
import io.liquidsoftware.base.booking.application.port.`in`.CancelAppointmentError
import io.liquidsoftware.base.booking.application.port.`in`.DateInPastError
import io.liquidsoftware.base.booking.application.port.`in`.DateTimeUnavailableError
import io.liquidsoftware.base.payment.application.port.`in`.PaymentDeclinedError
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodNotFoundError
import io.liquidsoftware.common.workflow.WorkflowError as LegacyWorkflowError
import io.liquidsoftware.workflow.WorkflowError as UseCaseError

internal fun mapBookingDomainError(domainError: UseCaseError.DomainError): LegacyWorkflowError? =
  when (domainError.code) {
    DATE_IN_PAST_CODE -> DateInPastError(domainError.message)
    DATE_TIME_UNAVAILABLE_CODE -> DateTimeUnavailableError(domainError.message)
    APPOINTMENT_VALIDATION_CODE -> AppointmentValidationError(domainError.message)
    APPOINTMENT_NOT_FOUND_CODE -> AppointmentNotFoundError(domainError.message)
    CANCEL_APPOINTMENT_CODE -> CancelAppointmentError(domainError.message)
    else -> null
  }

internal fun mapBookingOrPaymentDomainError(domainError: UseCaseError.DomainError): LegacyWorkflowError? =
  mapBookingDomainError(domainError) ?: when (domainError.code) {
    PAYMENT_METHOD_NOT_FOUND_CODE -> PaymentMethodNotFoundError(domainError.message)
    PAYMENT_DECLINED_CODE -> PaymentDeclinedError(domainError.message)
    else -> null
  }

internal const val DATE_IN_PAST_CODE = "DATE_IN_PAST"
internal const val DATE_TIME_UNAVAILABLE_CODE = "DATE_TIME_UNAVAILABLE"
internal const val APPOINTMENT_VALIDATION_CODE = "APPOINTMENT_VALIDATION"
internal const val APPOINTMENT_NOT_FOUND_CODE = "APPOINTMENT_NOT_FOUND"
internal const val CANCEL_APPOINTMENT_CODE = "CANCEL_APPOINTMENT"
internal const val PAYMENT_METHOD_NOT_FOUND_CODE = "PAYMENT_METHOD_NOT_FOUND"
internal const val PAYMENT_DECLINED_CODE = "PAYMENT_DECLINED"

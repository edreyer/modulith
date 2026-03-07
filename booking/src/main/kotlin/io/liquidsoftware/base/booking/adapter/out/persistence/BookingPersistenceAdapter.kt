package io.liquidsoftware.base.booking.adapter.out.persistence

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.Raise
import arrow.core.raise.context.raise
import arrow.core.raise.either
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentDtoOut
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentStatus
import io.liquidsoftware.base.booking.application.port.`in`.WorkOrderDtoOut
import io.liquidsoftware.base.booking.application.port.`in`.WorkOrderStatus
import io.liquidsoftware.base.booking.application.port.out.AppointmentEventPort
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.domain.Appointment
import io.liquidsoftware.base.booking.domain.CancelledAppointment
import io.liquidsoftware.base.booking.domain.CancelledWorkOrder
import io.liquidsoftware.base.booking.domain.CompleteAppointment
import io.liquidsoftware.base.booking.domain.CompleteWorkOrder
import io.liquidsoftware.base.booking.domain.InProgressAppointment
import io.liquidsoftware.base.booking.domain.InProgressWorkOrder
import io.liquidsoftware.base.booking.domain.PaidAppointment
import io.liquidsoftware.base.booking.domain.PaidWorkOrder
import io.liquidsoftware.base.booking.domain.ReadyWorkOrder
import io.liquidsoftware.base.booking.domain.ScheduledAppointment
import io.liquidsoftware.base.booking.domain.WorkOrder
import io.liquidsoftware.common.ext.toWorkflowError
import io.liquidsoftware.common.ext.withContextIO
import io.liquidsoftware.common.ext.workflowBoundary
import io.liquidsoftware.common.security.acl.Acl
import io.liquidsoftware.common.security.acl.AclRole
import io.liquidsoftware.common.security.spring.arrow.SpringSecurityAclChecker
import io.liquidsoftware.common.types.ValidationError
import io.liquidsoftware.common.types.ValidationErrors
import io.liquidsoftware.common.workflow.WorkflowError
import io.liquidsoftware.common.workflow.WorkflowValidationError
import org.springframework.data.domain.Pageable
import java.time.LocalDate

internal class BookingPersistenceAdapter(
  private val apptRepository: AppointmentRepository,
  private val ac: SpringSecurityAclChecker
) : FindAppointmentPort, AppointmentEventPort {

  override suspend fun findById(apptId: String): Either<WorkflowError, Appointment?> =
    withContextIO {
      either {
        val entity = workflowBoundary {
          apptRepository.findByAppointmentId(apptId)
        } ?: return@either null
        val appointment = entity.toAppointment().fold(
          { raise(WorkflowValidationError(it)) },
          { it }
        )
        ensureCanRead(appointment.acl())
        appointment
      }
    }

  override suspend fun findScheduledById(apptId: String): Either<WorkflowError, ScheduledAppointment?> =
    findById(apptId).fold(
      { it.left() },
      { appointment ->
        appointment
          ?.let { if (it is ScheduledAppointment) it else null }
          .right()
      }
    )

  override suspend fun findStartedById(apptId: String): Either<WorkflowError, InProgressAppointment?> =
    findById(apptId).fold(
      { it.left() },
      { appointment ->
        appointment
          ?.let { if (it is InProgressAppointment) it else null }
          .right()
      }
    )

  override suspend fun findCompletedById(apptId: String): Either<WorkflowError, CompleteAppointment?> =
    findById(apptId).fold(
      { it.left() },
      { appointment ->
        appointment
          ?.let { if (it is CompleteAppointment) it else null }
          .right()
      }
    )

  override suspend fun findByUserId(
    userId: String,
    pageable: Pageable
  ): Either<WorkflowError, List<Appointment>> =
    withContextIO {
      either {
        workflowBoundary {
          apptRepository.findByUserId(userId, pageable)
        }
          .map { entity ->
            entity.toAppointment()
              .fold(
                { raise(WorkflowValidationError(it)) },
                { it }
              )
          }
          .onEach { appointment -> ensureCanRead(appointment.acl()) }
      }
    }

  override suspend fun findAll(date: LocalDate): Either<WorkflowError, List<Appointment>> =
    withContextIO {
      either {
        workflowBoundary {
          apptRepository.findByScheduledTimeBetween(date.atStartOfDay(), date.atStartOfDay().plusDays(1))
        }
          .map { entity ->
            entity.toAppointment()
              .fold(
                { raise(WorkflowValidationError(it)) },
                { it }
              )
              .also { appointment -> ensureCanRead(appointment.acl()) }
          }
      }
    }

  override suspend fun <T : AppointmentEvent> handle(event: T): Either<WorkflowError, T> = withContextIO {
    either {
      when (event.appointmentDto.status) {
        AppointmentStatus.SCHEDULED,
        AppointmentStatus.IN_PROGRESS,
        AppointmentStatus.COMPLETE,
        AppointmentStatus.PAID -> {
          workflowBoundary {
            apptRepository.findByAppointmentId(event.appointmentDto.id)
          }
            ?.let {
              ensureCanWrite(it.acl())
              it.handle(event)
            }
            ?.let {
              workflowBoundary { apptRepository.save(it) }
            }
            ?: workflowBoundary { apptRepository.save(event.appointmentDto.toEntity()) }
          event
        }
        AppointmentStatus.CANCELLED -> {
          workflowBoundary {
            apptRepository.findByAppointmentId(event.appointmentDto.id)
          }
            ?.let {
              ensureCanWrite(Acl.of(it.appointmentId, it.userId, AclRole.WRITER))
              it.handle(event)
            }
            ?.let {
              workflowBoundary { apptRepository.save(it) }
            }
          event
        }
      }
    }
  }

  context(_: Raise<WorkflowError>)
  private suspend fun ensureCanRead(acl: Acl) {
    either {
      ac.ensureCanRead(acl)
    }.fold(
      { raise(it.toWorkflowError()) },
      {}
    )
  }

  context(_: Raise<WorkflowError>)
  private suspend fun ensureCanWrite(acl: Acl) {
    either {
      ac.ensureCanWrite(acl)
    }.fold(
      { raise(it.toWorkflowError()) },
      {}
    )
  }

  private fun AppointmentEntity.toAppointment(): Either<ValidationErrors, Appointment> {
    val entity = this
    return when (status) {
      AppointmentStatus.SCHEDULED -> either<ValidationErrors, Appointment> {
        ScheduledAppointment.of(
          entity.appointmentId,
          entity.userId,
          entity.scheduledTime,
          entity.duration,
          entity.workOrder.toWorkOrder().bind().asReadyWorkOrder(entity).bind()
        )
      }
      AppointmentStatus.IN_PROGRESS -> either<ValidationErrors, Appointment> {
        InProgressAppointment.of(
          entity.appointmentId,
          entity.userId,
          entity.scheduledTime,
          entity.duration,
          entity.workOrder.toWorkOrder().bind().asInProgressWorkOrder(entity).bind()
        )
      }
      AppointmentStatus.COMPLETE -> either<ValidationErrors, Appointment> {
        CompleteAppointment.of(
          entity.appointmentId,
          entity.userId,
          entity.scheduledTime,
          entity.duration,
          entity.workOrder.toWorkOrder().bind().asCompleteWorkOrder(entity).bind(),
          entity.requiredCompleteTime().bind()
        )
      }
      AppointmentStatus.PAID -> either<ValidationErrors, Appointment> {
        PaidAppointment.of(
          entity.appointmentId,
          entity.requiredPaymentId().bind(),
          entity.userId,
          entity.scheduledTime,
          entity.duration,
          entity.workOrder.toWorkOrder().bind().asPaidWorkOrder(entity).bind(),
          entity.requiredCompleteTime().bind()
        )
      }
      AppointmentStatus.CANCELLED -> either<ValidationErrors, Appointment> {
        CancelledAppointment.of(
          entity.appointmentId,
          entity.userId,
          entity.scheduledTime,
          entity.duration,
          entity.workOrder.toWorkOrder().bind(),
          entity.requiredCancelTime().bind()
        )
      }
    }
  }

  private fun AppointmentDtoOut.toEntity(): AppointmentEntity =
    AppointmentEntity(
      appointmentId = id,
      userId = this.userId,
      duration = this.duration,
      workOrder = this.workOrderDto.toEntity(),
      status = this.status,
      scheduledTime = this.scheduledTime,
      completeTime = this.completeTime,
      paymentId = this.paymentId,
      cancelTime = this.cancelTime
    )

  private fun WorkOrderEmbedded.toWorkOrder(): Either<ValidationErrors, WorkOrder> {
    val wo = this
    return when (status) {
      WorkOrderStatus.READY -> either<ValidationErrors, WorkOrder> {
        ReadyWorkOrder.of(wo.service, wo.notes)
      }
      WorkOrderStatus.IN_PROGRESS -> either<ValidationErrors, WorkOrder> {
        InProgressWorkOrder.of(wo.service, wo.requiredStartTime().bind())
      }
      WorkOrderStatus.COMPLETE -> either<ValidationErrors, WorkOrder> {
        CompleteWorkOrder.of(
          wo.service,
          wo.requiredStartTime().bind(),
          wo.requiredCompleteTime().bind(),
          wo.requiredNotes().bind()
        )
      }
      WorkOrderStatus.PAID -> either<ValidationErrors, WorkOrder> {
        PaidWorkOrder.of(
          wo.service,
          wo.requiredStartTime().bind(),
          wo.requiredCompleteTime().bind(),
          wo.requiredPaymentTime().bind(),
          wo.requiredNotes().bind()
        )
      }
      WorkOrderStatus.CANCELLED -> either<ValidationErrors, WorkOrder> {
        CancelledWorkOrder.of(
          wo.service,
          wo.requiredCancelTime().bind(),
          wo.requiredNotes().bind()
        )
      }
    }
  }

  private fun WorkOrderDtoOut.toEntity(): WorkOrderEmbedded = WorkOrderEmbedded(
    service = this.service,
    status = this.status,
    notes = this.notes,
    startTime = this.startTime,
    completeTime = this.completeTime,
    paymentTime = this.paymentTime,
    cancelTime = this.cancelTime
  )

  private fun AppointmentEntity.requiredCompleteTime() =
    completeTime.required("completeTime", "Appointment($appointmentId) is $status")

  private fun AppointmentEntity.requiredPaymentId() =
    paymentId.required("paymentId", "Appointment($appointmentId) is $status")

  private fun AppointmentEntity.requiredCancelTime() =
    cancelTime.required("cancelTime", "Appointment($appointmentId) is $status")

  private fun WorkOrderEmbedded.requiredStartTime() =
    startTime.required("startTime", "WorkOrder($service) is $status")

  private fun WorkOrderEmbedded.requiredCompleteTime() =
    completeTime.required("completeTime", "WorkOrder($service) is $status")

  private fun WorkOrderEmbedded.requiredPaymentTime() =
    paymentTime.required("paymentTime", "WorkOrder($service) is $status")

  private fun WorkOrderEmbedded.requiredCancelTime() =
    cancelTime.required("cancelTime", "WorkOrder($service) is $status")

  private fun WorkOrderEmbedded.requiredNotes() =
    notes.required("notes", "WorkOrder($service) is $status")

  private fun WorkOrder.asReadyWorkOrder(entity: AppointmentEntity): Either<ValidationErrors, ReadyWorkOrder> =
    when (this) {
      is ReadyWorkOrder -> right()
      else -> invalidState("Appointment(${entity.appointmentId}) is ${entity.status} but work order is ${this::class.simpleName}").left()
    }

  private fun WorkOrder.asInProgressWorkOrder(entity: AppointmentEntity): Either<ValidationErrors, InProgressWorkOrder> =
    when (this) {
      is InProgressWorkOrder -> right()
      else -> invalidState("Appointment(${entity.appointmentId}) is ${entity.status} but work order is ${this::class.simpleName}").left()
    }

  private fun WorkOrder.asCompleteWorkOrder(entity: AppointmentEntity): Either<ValidationErrors, CompleteWorkOrder> =
    when (this) {
      is CompleteWorkOrder -> right()
      else -> invalidState("Appointment(${entity.appointmentId}) is ${entity.status} but work order is ${this::class.simpleName}").left()
    }

  private fun WorkOrder.asPaidWorkOrder(entity: AppointmentEntity): Either<ValidationErrors, PaidWorkOrder> =
    when (this) {
      is PaidWorkOrder -> right()
      else -> invalidState("Appointment(${entity.appointmentId}) is ${entity.status} but work order is ${this::class.simpleName}").left()
    }

  private fun <T> T?.required(field: String, context: String): Either<ValidationErrors, T> =
    this?.right() ?: invalidState("$context but $field is missing").left()

  private fun invalidState(message: String): ValidationErrors =
    listOf(ValidationError(message)).toNonEmptyListOrNull()!!
}

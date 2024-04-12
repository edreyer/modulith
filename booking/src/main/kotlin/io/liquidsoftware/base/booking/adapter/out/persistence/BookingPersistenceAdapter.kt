package io.liquidsoftware.base.booking.adapter.out.persistence

import arrow.core.identity
import arrow.core.raise.either
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
import io.liquidsoftware.common.errors.ErrorHandling.ERROR_HANDLER
import io.liquidsoftware.common.ext.withContextIO
import io.liquidsoftware.common.logging.LoggerDelegate
import io.liquidsoftware.common.security.acl.Acl
import io.liquidsoftware.common.security.acl.AclChecker
import io.liquidsoftware.common.security.acl.AclRole
import io.liquidsoftware.common.security.acl.Permission
import io.liquidsoftware.common.types.ValidationErrors
import org.springframework.data.domain.Pageable
import java.time.LocalDate

internal class BookingPersistenceAdapter(
  private val apptRepository: AppointmentRepository,
  private val ac: AclChecker
) : FindAppointmentPort, AppointmentEventPort {

  private val logger by LoggerDelegate()

  override suspend fun findById(apptId: String): Appointment? =
    withContextIO {
      runCatching {
        apptRepository.findByAppointmentId(apptId)
          ?.toAppointment()
          ?.also { ac.checkPermission(it.acl(), Permission.READ) }
      }.getOrNull()
    }


  override suspend fun findScheduledById(apptId: String): ScheduledAppointment? =
    findById(apptId)
      ?.let { if (it !is ScheduledAppointment) null else it }

  override suspend fun findStartedById(apptId: String): InProgressAppointment? =
    findById(apptId)
      ?.let { if (it !is InProgressAppointment) null else it }

  override suspend fun findCompletedById(apptId: String): CompleteAppointment? =
    findById(apptId)
      ?.let { if (it !is CompleteAppointment) null else it }

  override suspend fun findByUserId(userId: String, pageable: Pageable): List<Appointment> =
    withContextIO {
      apptRepository.findByUserId(userId, pageable)
        .map { it.toAppointment() }
        .filter { appt -> runCatching {
          ac.checkPermission(appt.acl(), Permission.READ)
        }
          .fold({ true }, { false })
        }
    }

  override suspend fun findAll(date: LocalDate): List<Appointment> =
    withContextIO {
      apptRepository.findByScheduledTimeBetween(date.atStartOfDay(), date.atStartOfDay().plusDays(1))
        .map {
          it.toAppointment()
            .also { appt -> ac.checkPermission(appt.acl(), Permission.READ)}
        }
    }

  override suspend fun <T: AppointmentEvent> handle(event: T): T = withContextIO {
    when (event.appointmentDto.status) {
      AppointmentStatus.SCHEDULED,
      AppointmentStatus.IN_PROGRESS,
      AppointmentStatus.COMPLETE,
      AppointmentStatus.PAID -> {
        apptRepository.findByAppointmentId(event.appointmentDto.id)
          ?.also { ac.checkPermission(it.acl(), Permission.WRITE) }
          ?.handle(event)
          ?.let {
            apptRepository.save(it)
          }
          ?: apptRepository.save(event.appointmentDto.toEntity())
        event
      }
      else -> {
        // TODO: Do we need this 'else' branch? Delete if not
        apptRepository.findByAppointmentId(event.appointmentDto.id)
          ?.also { ac.checkPermission(Acl.of(it.appointmentId, it.userId, AclRole.WRITER), Permission.WRITE) }
          ?.handle(event)
          ?.let { apptRepository.save(it) }
        event
      }
    }
  }

  private fun AppointmentEntity.toAppointment(): Appointment {
    val entity = this
    return when (this.status) {
      AppointmentStatus.SCHEDULED -> either<ValidationErrors, Appointment> { ScheduledAppointment.of(
        entity.appointmentId, entity.userId, entity.scheduledTime, entity.duration,
        entity.workOrder.toWorkOrder() as ReadyWorkOrder
        )}.fold(ERROR_HANDLER, ::identity)
      AppointmentStatus.IN_PROGRESS -> either<ValidationErrors, Appointment> { InProgressAppointment.of(
        entity.appointmentId, entity.userId, entity.scheduledTime, entity.duration,
          workOrder.toWorkOrder() as InProgressWorkOrder
        )}.fold(ERROR_HANDLER, ::identity)
      AppointmentStatus.COMPLETE -> either<ValidationErrors, Appointment> { CompleteAppointment.of(
        entity.appointmentId, entity.userId, entity.scheduledTime, entity.duration,
        workOrder.toWorkOrder() as CompleteWorkOrder, entity.completeTime!!
      )}.fold(ERROR_HANDLER, ::identity)
      AppointmentStatus.PAID -> either<ValidationErrors, Appointment> { PaidAppointment.of(
        entity.appointmentId, entity.paymentId!!, entity.userId, entity.scheduledTime, entity.duration,
        workOrder.toWorkOrder() as PaidWorkOrder, entity.completeTime!!
      )}.fold(ERROR_HANDLER, ::identity)
      AppointmentStatus.CANCELLED -> either<ValidationErrors, Appointment> { CancelledAppointment.of(
        entity.appointmentId, entity.userId, entity.scheduledTime, entity.duration, workOrder.toWorkOrder(), entity.cancelTime!!
      )}.fold(ERROR_HANDLER, ::identity)
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


  private fun WorkOrderEmbedded.toWorkOrder(): WorkOrder {
    val wo = this
    return when (this.status) {
      WorkOrderStatus.READY -> either { ReadyWorkOrder.of(wo.service, wo.notes) }
        .fold(ERROR_HANDLER, ::identity)
      WorkOrderStatus.IN_PROGRESS -> either { InProgressWorkOrder.of(
        wo.service, wo.startTime!!
      )}.fold(ERROR_HANDLER, ::identity)
      WorkOrderStatus.COMPLETE -> either { CompleteWorkOrder.of(
        wo.service, wo.startTime!!, wo.completeTime!!, wo.notes!!
      )}.fold(ERROR_HANDLER, ::identity)
      WorkOrderStatus.PAID -> either { PaidWorkOrder.of(
        wo.service, wo.startTime!!, wo.completeTime!!, wo.paymentTime!!, wo.notes!!
      )}.fold(ERROR_HANDLER, ::identity)
      WorkOrderStatus.CANCELLED -> either { CancelledWorkOrder.of(
        wo.service, wo.cancelTime!!, wo.notes!!
      )}.fold(ERROR_HANDLER, ::identity)
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

}

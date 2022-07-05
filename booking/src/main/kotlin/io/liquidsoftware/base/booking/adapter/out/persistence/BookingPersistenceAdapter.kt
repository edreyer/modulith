package io.liquidsoftware.base.booking.adapter.out.persistence

import arrow.core.Nel
import arrow.core.identity
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
import io.liquidsoftware.base.booking.domain.ReadyWorkOrder
import io.liquidsoftware.base.booking.domain.ScheduledAppointment
import io.liquidsoftware.base.booking.domain.WorkOrder
import io.liquidsoftware.common.logging.LoggerDelegate
import io.liquidsoftware.common.security.acl.Acl
import io.liquidsoftware.common.security.acl.AclChecker
import io.liquidsoftware.common.security.acl.AclRole
import io.liquidsoftware.common.security.acl.Permission
import io.liquidsoftware.common.types.ValidationError
import io.liquidsoftware.common.types.ValidationException
import io.liquidsoftware.common.types.toErrString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

internal class BookingPersistenceAdapter(
  private val apptRepository: AppointmentRepository,
  private val workOrderRepository: WorkOrderRepository,
  private val ac: AclChecker
) : FindAppointmentPort, AppointmentEventPort {

  private val logger by LoggerDelegate()

  override suspend fun findById(apptId: String): Appointment? =
    withContext(Dispatchers.IO) {
      apptRepository.findByAppointmentId(apptId)
        ?.toAppointment()
        ?.also { ac.checkPermission(it.acl(), Permission.READ)}
    }

  override suspend fun findScheduledById(apptId: String): ScheduledAppointment? =
    findById(apptId)
      ?.let { if (it !is ScheduledAppointment) null else it }

  override suspend fun findStartedById(apptId: String): InProgressAppointment? =
    findById(apptId)
      ?.let { if (it !is InProgressAppointment) null else it }

  override suspend fun findByUserId(userId: String): List<Appointment> =
    withContext(Dispatchers.IO) {
      apptRepository.findByUserId(userId)
        .map {
          it.toAppointment()
            .also { appt -> ac.checkPermission(appt.acl(), Permission.READ)}
        }
    }

  override suspend fun findAll(date: LocalDate): List<Appointment> =
    withContext(Dispatchers.IO) {
      apptRepository.findByScheduledTimeBetween(date.atStartOfDay(), date.atStartOfDay().plusDays(1))
        .map {
          it.toAppointment()
            .also { appt -> ac.checkPermission(appt.acl(), Permission.READ)}
        }
    }

  override suspend fun <T: AppointmentEvent> handle(event: T): T = withContext(Dispatchers.IO) {
    when (event.appointmentDto.status) {
      AppointmentStatus.SCHEDULED,
      AppointmentStatus.IN_PROGRESS,
      AppointmentStatus.COMPLETE -> {
        apptRepository.findByAppointmentId(event.appointmentDto.id)
          ?.also { ac.checkPermission(it.acl(), Permission.WRITE) }
          ?.handle(event)
          ?.let {
            workOrderRepository.saveAndFlush(it.workOrder)
            apptRepository.saveAndFlush(it)
          }
          ?: apptRepository.saveAndFlush(event.appointmentDto.toEntity()) // New Entity
        event
      }
      else -> {
        apptRepository.findByAppointmentId(event.appointmentDto.id)
          ?.also { ac.checkPermission(Acl.of(it.id, it.userId, AclRole.WRITER), Permission.WRITE) }
          ?.handle(event)
          ?.let { apptRepository.saveAndFlush(it) }
        event
      }
    }
  }

  private fun AppointmentEntity.toAppointment(): Appointment {
    val errorHandler = { errors: Nel<ValidationError> ->
      val err = errors.toErrString()
      logger.error(err)
      throw ValidationException(errors)
    }
    return when (this.status) {
      AppointmentStatus.SCHEDULED -> ScheduledAppointment.of(
          this.id, this.userId, this.scheduledTime, this.duration,
          this.workOrder.toWorkOrder() as ReadyWorkOrder
        ).fold(errorHandler, ::identity)
      AppointmentStatus.IN_PROGRESS -> InProgressAppointment.of(
          this.id, this.userId, this.scheduledTime, this.duration,
          this.workOrder.toWorkOrder() as InProgressWorkOrder
        ).fold(errorHandler, ::identity)
      AppointmentStatus.COMPLETE -> CompleteAppointment.of(
        this.id, this.userId, this.scheduledTime, this.duration,
        this.workOrder.toWorkOrder() as CompleteWorkOrder, this.completeTime!!
      ).fold(errorHandler, ::identity)
      AppointmentStatus.CANCELLED -> CancelledAppointment.of(
        this.id, this.userId, this.scheduledTime, this.duration, this.workOrder.toWorkOrder(), this.cancelTime!!
      ).fold(errorHandler, ::identity)
    }
  }

  private fun AppointmentDtoOut.toEntity(): AppointmentEntity =
    apptRepository.findById(id).orElseGet {
      AppointmentEntity(
        apptId = id,
        userId = this.userId,
        duration = this.duration,
        workOrder = this.workOrderDto.toEntity(),
        status = this.status,
        scheduledTime = this.scheduledTime,
        completeTime = this.completeTime,
        cancelTime = this.cancelTime
      )
    }

  private fun WorkOrderEntity.toWorkOrder(): WorkOrder {
    val errorHandler = { errors: Nel<ValidationError> ->
      val err = errors.toErrString()
      logger.error(err)
      throw ValidationException(errors)
    }
    return when (this.status) {
      WorkOrderStatus.READY -> ReadyWorkOrder.of(this.id, this.service)
        .fold(errorHandler, ::identity)
      WorkOrderStatus.IN_PROGRESS -> InProgressWorkOrder.of(
        this.id, this.service, this.startTime!!
      ).fold(errorHandler, ::identity)
      WorkOrderStatus.COMPLETE -> CompleteWorkOrder.of(
        this.id, this.service, this.startTime!!, this.completeTime!!, this.notes!!
      ).fold(errorHandler, ::identity)
      WorkOrderStatus.CANCELLED -> CancelledWorkOrder.of(
        this.id, this.service, this.cancelTime!!, this.notes!!
      ).fold(errorHandler, ::identity)
    }
  }

  private fun WorkOrderDtoOut.toEntity(): WorkOrderEntity = this.id.let { id ->
    workOrderRepository.findById(id).orElseGet {
      WorkOrderEntity(
        workOrderId = id,
        service = this.service,
        status = this.status,
        notes = this.notes,
        startTime = this.startTime,
        completeTime = this.completeTime,
        cancelTime = this.cancelTime
      )
    }
  }

}

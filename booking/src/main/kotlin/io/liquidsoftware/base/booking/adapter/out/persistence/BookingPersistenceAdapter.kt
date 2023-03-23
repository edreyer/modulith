package io.liquidsoftware.base.booking.adapter.out.persistence

import arrow.core.continuations.effect
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
import io.liquidsoftware.base.booking.domain.PaidAppointment
import io.liquidsoftware.base.booking.domain.PaidWorkOrder
import io.liquidsoftware.base.booking.domain.ReadyWorkOrder
import io.liquidsoftware.base.booking.domain.ScheduledAppointment
import io.liquidsoftware.base.booking.domain.WorkOrder
import io.liquidsoftware.common.errors.ErrorHandling.ERROR_HANDLER
import io.liquidsoftware.common.logging.LoggerDelegate
import io.liquidsoftware.common.security.acl.Acl
import io.liquidsoftware.common.security.acl.AclChecker
import io.liquidsoftware.common.security.acl.AclRole
import io.liquidsoftware.common.security.acl.Permission
import io.liquidsoftware.common.types.ValidationErrors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import java.time.LocalDate

internal class BookingPersistenceAdapter(
  private val apptRepository: AppointmentRepository,
  private val ac: AclChecker
) : FindAppointmentPort, AppointmentEventPort {

  private val logger by LoggerDelegate()

  override suspend fun findById(apptId: String): Appointment? =
    withContext(Dispatchers.IO) {
      apptRepository.findByAppointmentId(apptId)
        .awaitSingleOrNull()
        ?.toAppointment()
    }?.also { ac.checkPermission(it.acl(), Permission.READ) }


  override suspend fun findScheduledById(apptId: String): ScheduledAppointment? =
    findById(apptId)
      ?.let { if (it !is ScheduledAppointment) null else it }

  override suspend fun findStartedById(apptId: String): InProgressAppointment? =
    findById(apptId)
      ?.let { if (it !is InProgressAppointment) null else it }

  override suspend fun findCompletedById(apptId: String): CompleteAppointment? =
    findById(apptId)
      ?.let { if (it !is CompleteAppointment) null else it }

  override suspend fun findByUserId(userId: String): List<Appointment> =
    withContext(Dispatchers.IO) {
      apptRepository.findByUserId(userId)
        .asFlow()
        .toList()
        .map {
          it.toAppointment()
            .also { appt -> ac.checkPermission(appt.acl(), Permission.READ)}
        }
    }

  override suspend fun findAll(date: LocalDate): List<Appointment> =
    withContext(Dispatchers.IO) {
      apptRepository.findByScheduledTimeBetween(date.atStartOfDay(), date.atStartOfDay().plusDays(1))
        .asFlow()
        .toList()
        .map {
          it.toAppointment()
            .also { appt -> ac.checkPermission(appt.acl(), Permission.READ)}
        }
    }

  override suspend fun <T: AppointmentEvent> handle(event: T): T = withContext(Dispatchers.IO) {
    when (event.appointmentDto.status) {
      AppointmentStatus.SCHEDULED,
      AppointmentStatus.IN_PROGRESS,
      AppointmentStatus.COMPLETE,
      AppointmentStatus.PAID -> {
        apptRepository.findByAppointmentId(event.appointmentDto.id)
          .awaitSingleOrNull()
          ?.also { ac.checkPermission(it.acl(), Permission.WRITE) }
          ?.handle(event)
          ?.let {
            apptRepository.save(it).awaitSingle()
          }
          ?: apptRepository.save(event.appointmentDto.toEntity()).awaitSingle() // New Entity
        event
      }
      else -> {
        // TODO: Do we need this 'else' branch? Delete if not
        apptRepository.findByAppointmentId(event.appointmentDto.id)
          .awaitSingleOrNull()
          ?.also { ac.checkPermission(Acl.of(it.appointmentId, it.userId, AclRole.WRITER), Permission.WRITE) }
          ?.handle(event)
          ?.let { apptRepository.save(it).awaitSingle() }
        event
      }
    }
  }

  private suspend fun AppointmentEntity.toAppointment(): Appointment {
    val entity = this;
    return when (this.status) {
      AppointmentStatus.SCHEDULED -> effect<ValidationErrors, Appointment> { ScheduledAppointment.of(
        entity.appointmentId, entity.userId, entity.scheduledTime, entity.duration,
        entity.workOrder.toWorkOrder() as ReadyWorkOrder
        )}.fold(ERROR_HANDLER, ::identity)
      AppointmentStatus.IN_PROGRESS -> effect<ValidationErrors, Appointment> { InProgressAppointment.of(
        entity.appointmentId, entity.userId, entity.scheduledTime, entity.duration,
          workOrder.toWorkOrder() as InProgressWorkOrder
        )}.fold(ERROR_HANDLER, ::identity)
      AppointmentStatus.COMPLETE -> effect<ValidationErrors, Appointment> { CompleteAppointment.of(
        entity.appointmentId, entity.userId, entity.scheduledTime, entity.duration,
        workOrder.toWorkOrder() as CompleteWorkOrder, entity.completeTime!!
      )}.fold(ERROR_HANDLER, ::identity)
      AppointmentStatus.PAID -> effect<ValidationErrors, Appointment> { PaidAppointment.of(
        entity.appointmentId, entity.paymentId!!, entity.userId, entity.scheduledTime, entity.duration,
        workOrder.toWorkOrder() as PaidWorkOrder, entity.completeTime!!
      )}.fold(ERROR_HANDLER, ::identity)
      AppointmentStatus.CANCELLED -> effect<ValidationErrors, Appointment> { CancelledAppointment.of(
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


  private suspend fun WorkOrderEmbedded.toWorkOrder(): WorkOrder {
    val wo = this
    return when (this.status) {
      WorkOrderStatus.READY -> effect { ReadyWorkOrder.of(wo.service, wo.notes) }
        .fold(ERROR_HANDLER, ::identity)
      WorkOrderStatus.IN_PROGRESS -> effect { InProgressWorkOrder.of(
        wo.service, wo.startTime!!
      )}.fold(ERROR_HANDLER, ::identity)
      WorkOrderStatus.COMPLETE -> effect { CompleteWorkOrder.of(
        wo.service, wo.startTime!!, wo.completeTime!!, wo.notes!!
      )}.fold(ERROR_HANDLER, ::identity)
      WorkOrderStatus.PAID -> effect { PaidWorkOrder.of(
        wo.service, wo.startTime!!, wo.completeTime!!, wo.paymentTime!!, wo.notes!!
      )}.fold(ERROR_HANDLER, ::identity)
      WorkOrderStatus.CANCELLED -> effect { CancelledWorkOrder.of(
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

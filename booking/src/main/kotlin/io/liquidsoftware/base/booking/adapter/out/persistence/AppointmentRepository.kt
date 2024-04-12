package io.liquidsoftware.base.booking.adapter.out.persistence

import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
internal interface AppointmentRepository : MongoRepository<AppointmentEntity, String> {

  fun findByAppointmentId(appointmentId: String) : AppointmentEntity?

  fun findByUserId(userId: String, pageable: Pageable): List<AppointmentEntity>

  fun findByScheduledTimeBetween(start: LocalDateTime, end: LocalDateTime): List<AppointmentEntity>

}

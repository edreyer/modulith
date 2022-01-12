package io.liquidsoftware.base.booking.adapter.out.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
internal interface AppointmentRepository : JpaRepository<io.liquidsoftware.base.booking.adapter.out.persistence.AppointmentEntity, String> {

  @Query(value = "from AppointmentEntity where id = :appointmentId")
  fun findByAppointmentId(appointmentId: String) : io.liquidsoftware.base.booking.adapter.out.persistence.AppointmentEntity?

  fun findByUserId(userId: String): List<io.liquidsoftware.base.booking.adapter.out.persistence.AppointmentEntity>

  fun findByStartTimeBetween(start: LocalDateTime, end: LocalDateTime): List<io.liquidsoftware.base.booking.adapter.out.persistence.AppointmentEntity>

}

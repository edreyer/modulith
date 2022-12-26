package io.liquidsoftware.base.booking.adapter.out.persistence

import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Repository
internal interface AppointmentRepository : ReactiveMongoRepository<AppointmentEntity, String> {

  fun findByAppointmentId(appointmentId: String) : Mono<AppointmentEntity>

  fun findByUserId(userId: String): Flux<AppointmentEntity>

  fun findByScheduledTimeBetween(start: LocalDateTime, end: LocalDateTime): Flux<AppointmentEntity>

}

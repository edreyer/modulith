package io.liquidsoftware.base.booking.adapter.out.persistence

import arrow.core.Either
import assertk.assertThat
import assertk.assertions.isTrue
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentStatus
import io.liquidsoftware.base.booking.application.port.`in`.WorkOrderStatus
import io.liquidsoftware.common.security.ExecutionContext
import io.liquidsoftware.common.security.UserDetailsWithId
import io.liquidsoftware.common.security.acl.AclChecker
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.data.domain.Pageable
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import java.lang.reflect.Proxy
import java.time.LocalDateTime

class BookingPersistenceAdapterArrowContractTest {

  @AfterEach
  fun clearSecurityContext() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun `findByUserId returns left for inconsistent persisted appointment state`() = runBlocking {
    val userId = "u_test-user"
    authenticate(userId)
    val entity = AppointmentEntity(
      appointmentId = "a_test-appointment",
      userId = userId,
      workOrder = WorkOrderEmbedded(
        service = "Oil Change",
        status = WorkOrderStatus.IN_PROGRESS,
        startTime = LocalDateTime.now()
      ),
      scheduledTime = LocalDateTime.now(),
      duration = 30,
      status = AppointmentStatus.SCHEDULED
    )
    val adapter = BookingPersistenceAdapter(
      appointmentRepository(findByUserId = { _, _ -> listOf(entity) }),
      AclChecker(ExecutionContext())
    )

    val result = adapter.findByUserId(userId, Pageable.unpaged())

    assertThat(result is Either.Left).isTrue()
  }

  @Test
  fun `findByUserId returns left when acl check fails`() = runBlocking {
    val entity = AppointmentEntity(
      appointmentId = "a_test-appointment",
      userId = "u_owner",
      workOrder = WorkOrderEmbedded(
        service = "Oil Change",
        status = WorkOrderStatus.READY,
        notes = "ready"
      ),
      scheduledTime = LocalDateTime.now(),
      duration = 30,
      status = AppointmentStatus.SCHEDULED
    )
    authenticate("u_other-user")
    val adapter = BookingPersistenceAdapter(
      appointmentRepository(findByUserId = { _, _ -> listOf(entity) }),
      AclChecker(ExecutionContext())
    )

    val result = adapter.findByUserId("u_other-user", Pageable.unpaged())

    assertThat(result is Either.Left).isTrue()
  }

  @Test
  fun `findById returns left when repository fails`() = runBlocking {
    val adapter = BookingPersistenceAdapter(
      appointmentRepository(findByAppointmentId = { throw DataAccessResourceFailureException("db down") }),
      AclChecker(ExecutionContext())
    )

    val result = adapter.findById("a_test-appointment")

    assertThat(result is Either.Left).isTrue()
  }

  @Test
  fun `findByUserId returns left when repository fails`() = runBlocking {
    authenticate("u_test-user")
    val adapter = BookingPersistenceAdapter(
      appointmentRepository(findByUserId = { _, _ -> throw DataAccessResourceFailureException("db down") }),
      AclChecker(ExecutionContext())
    )

    val result = adapter.findByUserId("u_test-user", Pageable.unpaged())

    assertThat(result is Either.Left).isTrue()
  }

  private fun authenticate(userId: String) {
    val principal = UserDetailsWithId(
      userId,
      User(userId, "password", emptyList())
    )
    SecurityContextHolder.getContext().authentication =
      UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
  }

  private fun appointmentRepository(
    findByUserId: (String, Pageable) -> List<AppointmentEntity> = { _, _ -> emptyList() },
    findByAppointmentId: (String) -> AppointmentEntity? = { null }
  ): AppointmentRepository =
    Proxy.newProxyInstance(
      AppointmentRepository::class.java.classLoader,
      arrayOf(AppointmentRepository::class.java)
    ) { _, method, args ->
      when (method.name) {
        "findByUserId" -> findByUserId(args[0] as String, args[1] as Pageable)
        "findByAppointmentId" -> findByAppointmentId(args[0] as String)
        "findByScheduledTimeBetween" -> emptyList<AppointmentEntity>()
        "save" -> args[0]
        "toString" -> "AppointmentRepositoryProxy"
        "hashCode" -> System.identityHashCode(this)
        "equals" -> false
        else -> error("Unexpected repository method: ${method.name}")
      }
    } as AppointmentRepository
}

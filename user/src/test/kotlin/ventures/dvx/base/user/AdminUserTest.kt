package ventures.dvx.base.user

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import io.mockk.every
import io.mockk.mockk
import org.axonframework.test.aggregate.AggregateTestFixture
import org.axonframework.test.aggregate.FixtureConfiguration
import org.axonframework.test.matchers.Matchers.exactSequenceOf
import org.axonframework.test.matchers.Matchers.matches
import org.axonframework.test.matchers.Matchers.messageWithPayload
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import ventures.dvx.base.user.api.AdminUserId
import ventures.dvx.base.user.api.AdminUserRegisteredEvent
import ventures.dvx.base.user.api.RegisterAdminUserCommand
import ventures.dvx.base.user.command.AdminUser
import ventures.dvx.base.user.command.UserRole
import ventures.dvx.common.axon.command.persistence.IndexRepository

class AdminUserTest {

  lateinit var fixture: FixtureConfiguration<AdminUser>
  lateinit var indexRepository: IndexRepository

  @BeforeEach
  fun setup() {
    fixture = AggregateTestFixture(AdminUser::class.java)

    indexRepository = mockk()
    fixture.registerInjectableResource(indexRepository)
    fixture.registerInjectableResource(BCryptPasswordEncoder())
  }

  @Test
  fun shouldRegisterAdmin() {
    val userId = AdminUserId()

    every { indexRepository.findEntityByAggregateNameAndKey(AdminUser.aggregateName(), any()) } returns null

    fixture.givenNoPriorActivity()
      .`when`(RegisterAdminUserCommand(
        userId = userId,
        plainPassword = "password",
        email = "email@email.com",
        firstName = "admin",
        lastName = "admin"
      ))
      .expectSuccessfulHandlerExecution()
      .expectState {
        assertThat(it.id).isEqualTo(userId)
        assertThat(it.email).isEqualTo("email@email.com")
        assertThat(it.roles).contains(UserRole.ADMIN)
      }
      .expectEventsMatching(
        exactSequenceOf(
          messageWithPayload(
            matches { event: AdminUserRegisteredEvent ->
              event.email == "email@email.com"
                && event.userId == userId
            }
          )
        )
      )
  }


}

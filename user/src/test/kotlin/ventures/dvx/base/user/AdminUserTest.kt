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
import ventures.dvx.common.types.toEmailAddress
import ventures.dvx.common.types.toNonEmptyString

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
        email = "email@email.com".toEmailAddress(),
        plainPassword = "password".toNonEmptyString(),
        firstName = "admin".toNonEmptyString(),
        lastName = "admin".toNonEmptyString()
      ))
      .expectSuccessfulHandlerExecution()
      .expectState {
        assertThat(it.id).isEqualTo(userId)
        assertThat(it.email.value).isEqualTo("email@email.com")
        assertThat(it.roles).contains(UserRole.ADMIN)
      }
      .expectEventsMatching(
        exactSequenceOf(
          messageWithPayload(
            matches { event: AdminUserRegisteredEvent ->
              event.email.value == "email@email.com"
                && event.userId == userId
            }
          )
        )
      )
  }


}

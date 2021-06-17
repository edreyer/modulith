package ventures.dvx.base.user

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
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
import ventures.dvx.base.user.api.EndUserId
import ventures.dvx.base.user.api.EndUserLoginStartedEvent
import ventures.dvx.base.user.api.LoginEndUserCommand
import ventures.dvx.base.user.api.RegisterEndUserCommand
import ventures.dvx.base.user.api.TokenValidatedEvent
import ventures.dvx.base.user.api.UserRegistrationStartedEvent
import ventures.dvx.base.user.api.ValidateEndUserTokenCommand
import ventures.dvx.base.user.command.EndUser
import ventures.dvx.base.user.command.UserRole
import ventures.dvx.common.axon.IndexableAggregateDto
import ventures.dvx.common.axon.command.persistence.IndexRepository
import ventures.dvx.common.validation.MsisdnParser
import java.time.Clock
import java.time.Instant

class EndUserTest {

  lateinit var fixture: FixtureConfiguration<EndUser>
  lateinit var indexRepository: IndexRepository

  val userId = EndUserId()
  val registerUserCommand = RegisterEndUserCommand(
    userId = userId,
    msisdn = "+15125551212",
    email = "email@email.com",
    firstName = "admin",
    lastName = "admin"
  )
  val userRegistrationStartedEvent = UserRegistrationStartedEvent(
    ia = IndexableAggregateDto(EndUser.aggregateName(), userId.id, "+15125551212"),
    userId = userId,
    msisdn = "+15125551212",
    email = "email@email.com",
    firstName = "User",
    lastName = "User"
  )

  @BeforeEach
  fun setup() {
    fixture = AggregateTestFixture(EndUser::class.java)

    indexRepository = mockk()
    fixture.registerInjectableResource(indexRepository)

    fixture.registerInjectableResource(BCryptPasswordEncoder())
    fixture.registerInjectableResource(MsisdnParser())
    val clock: Clock = mockk()
    every { clock.instant() } returns Instant.now()
    fixture.registerInjectableResource(clock)
  }

  @Test
  fun shouldRegisterEndUser() {
    every { indexRepository.findEntityByAggregateNameAndKey(EndUser.aggregateName(), any()) } returns null

    fixture.givenNoPriorActivity()
      .`when`(registerUserCommand)
      .expectSuccessfulHandlerExecution()
      .expectState {
        assertThat(it.id).isEqualTo(userId)
        assertThat(it.email).isEqualTo("email@email.com")
        assertThat(it.roles).contains(UserRole.USER)
        assertThat(it.token?.token).isEqualTo("1234")
      }
      .expectEventsMatching(
        exactSequenceOf(
          messageWithPayload(
            matches { event: UserRegistrationStartedEvent ->
              event.email == "email@email.com"
                && event.userId == userId
            }
          )
        )
      )
  }

  @Test
  fun shouldLoginEndUser() {
    fixture.given(userRegistrationStartedEvent)
      .`when`(LoginEndUserCommand(userId = userId, msisdn = "+15125551212"))
      .expectSuccessfulHandlerExecution()
      .expectState {
        assertThat(it.token?.token).isEqualTo("1234")
      }
      .expectEvents(EndUserLoginStartedEvent(userId))
  }

  @Test
  fun shouldValidateTokenTest() {
    fixture.given(
      userRegistrationStartedEvent,
      EndUserLoginStartedEvent(userId)
    )
      .`when`(ValidateEndUserTokenCommand(userId = userId, msisdn = "+15125551212", token = "1234"))
      .expectSuccessfulHandlerExecution()
      .expectState {
        assertThat(it.token).isNull()
      }
      .expectEvents(TokenValidatedEvent())

  }

}

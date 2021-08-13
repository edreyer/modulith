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
import ventures.dvx.base.user.command.MsisdnToken
import ventures.dvx.base.user.command.UserRole
import ventures.dvx.base.user.config.UserConfig
import ventures.dvx.bridgekeeper.BridgeKeeper
import ventures.dvx.common.axon.IndexableAggregateDto
import ventures.dvx.common.axon.command.persistence.IndexJpaEntity
import ventures.dvx.common.axon.command.persistence.IndexRepository
import ventures.dvx.common.axon.security.AccessControlCommandDispatchInterceptor
import ventures.dvx.common.config.CommonConfig
import ventures.dvx.common.types.toEmailAddress
import ventures.dvx.common.types.toMsisdn
import ventures.dvx.common.types.toNonEmptyString
import ventures.dvx.common.validation.MsisdnParser
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit.HOURS
import java.util.*

class EndUserTest {

  private lateinit var fixture: FixtureConfiguration<EndUser>
  private lateinit var indexRepository: IndexRepository

  private val userConfig = mockk<UserConfig>()
  private val userId = EndUserId()
  private val registerUserCommand = RegisterEndUserCommand(
    userId = userId,
    msisdn = "+15125551212".toMsisdn(),
    email = "email@email.com".toEmailAddress(),
    firstName = "admin".toNonEmptyString(),
    lastName = "admin".toNonEmptyString()
  )
  private val token = MsisdnToken(
    "1234", "+15125551212".toMsisdn(), "email@email.com".toEmailAddress(), Date().toInstant().plus(1, HOURS)
  )
  private val userRegistrationStartedEvent = UserRegistrationStartedEvent(
    ia = IndexableAggregateDto(EndUser.aggregateName(), userId.id, "+15125551212"),
    token = token,
    userId = userId,
    firstName = "User".toNonEmptyString(),
    lastName = "User".toNonEmptyString()
  )

  @BeforeEach
  fun setup() {
    fixture = AggregateTestFixture(EndUser::class.java)

    fixture.registerInjectableResource(mockk<BridgeKeeper>())

    indexRepository = mockk()
    fixture.registerInjectableResource(indexRepository)

    fixture.registerInjectableResource(CommonConfig("dev"))

    fixture.registerInjectableResource(userConfig)
    every { userConfig.forcedMsisdnToken } returns "1234"

    fixture.registerInjectableResource(BCryptPasswordEncoder())
    fixture.registerInjectableResource(MsisdnParser())

    val clock: Clock = mockk()
    every { clock.instant() } returns Instant.now()
    fixture.registerInjectableResource(clock)

    fixture.registerIgnoredField(EndUserLoginStartedEvent::class.java, "token")

    fixture.registerCommandDispatchInterceptor(AccessControlCommandDispatchInterceptor(mapOf()))
  }

  @Test
  fun shouldRegisterEndUser() {
    every {
      indexRepository.findEntityByAggregateNameAndKey(EndUser.aggregateName(), any())
    } returns null

    fixture.givenNoPriorActivity()
      .`when`(registerUserCommand)
      .expectSuccessfulHandlerExecution()
      .expectState {
        assertThat(it.id).isEqualTo(userId)
        assertThat(it.email.value).isEqualTo("email@email.com")
        assertThat(it.roles).contains(UserRole.USER)
        assertThat(it.token?.token).isEqualTo("1234")
      }
      .expectEventsMatching(
        exactSequenceOf(
          messageWithPayload(
            matches { event: UserRegistrationStartedEvent ->
              event.token.email.value == "email@email.com"
                && event.userId == userId
            }
          )
        )
      )
  }

  @Test
  fun shouldLoginEndUser() {
    every {
      indexRepository.findEntityByAggregateNameAndKey(any(), any())
    } returns IndexJpaEntity(userId.id, EndUser.aggregateName(), "+15125551212")

    fixture.given(userRegistrationStartedEvent)
      .`when`(LoginEndUserCommand(userId = userId, msisdn = "+15125551212".toMsisdn()))
      .expectSuccessfulHandlerExecution()
      .expectState {
        assertThat(it.token?.token).isEqualTo("1234")
      }
      .expectEvents(EndUserLoginStartedEvent(token))
  }

  @Test
  fun shouldValidateTokenTest() {
    every {
      indexRepository.findEntityByAggregateNameAndKey(any(), any())
    } returns IndexJpaEntity(userId.id, EndUser.aggregateName(), "+15125551212")

    fixture.given(
      userRegistrationStartedEvent,
      EndUserLoginStartedEvent(token)
    )
      .`when`(ValidateEndUserTokenCommand(
        userId = userId, msisdn = "+15125551212".toMsisdn(), token = "1234".toNonEmptyString())
      )
      .expectSuccessfulHandlerExecution()
      .expectState {
        assertThat(it.token).isNull()
      }
      .expectEvents(TokenValidatedEvent())
  }

}

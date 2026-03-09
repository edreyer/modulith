package io.liquidsoftware.base.user.application.workflows

import arrow.core.Either
import io.liquidsoftware.base.user.application.port.`in`.UserEvent
import io.liquidsoftware.base.user.application.port.`in`.UserRegisteredEvent
import io.liquidsoftware.base.user.application.port.out.UserEventPort
import io.liquidsoftware.common.security.UserDetailsWithId
import io.liquidsoftware.common.workflow.WorkflowError
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User as SpringUser
import org.springframework.security.crypto.password.PasswordEncoder

internal fun authentication(userId: String, role: String): UsernamePasswordAuthenticationToken {
  val userDetails = UserDetailsWithId(
    id = userId,
    user = SpringUser(
      "caller@liquidsoftware.io",
      "",
      listOf(SimpleGrantedAuthority(role)),
    )
  )
  return UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
}

internal fun passwordEncoder(encode: (String) -> String?): PasswordEncoder = object : PasswordEncoder {
  override fun encode(rawPassword: CharSequence?): String? = encode(checkNotNull(rawPassword).toString())

  override fun matches(rawPassword: CharSequence?, encodedPassword: String?): Boolean =
    encodedPassword == encode(rawPassword?.toString().orEmpty())
}

internal fun userEventPort(): UserEventPort = object : UserEventPort {
  override suspend fun handle(event: UserRegisteredEvent): Either<WorkflowError, UserRegisteredEvent> =
    Either.Right(event)

  override suspend fun <T : UserEvent> handle(event: T): Either<WorkflowError, T> =
    Either.Right(event)
}

internal fun failingUserEventPort(): UserEventPort = object : UserEventPort {
  override suspend fun handle(event: UserRegisteredEvent): Either<WorkflowError, UserRegisteredEvent> =
    error("user event port should not be invoked")

  override suspend fun <T : UserEvent> handle(event: T): Either<WorkflowError, T> =
    error("user event port should not be invoked")
}

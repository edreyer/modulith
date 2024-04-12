package io.liquidsoftware.common.security

import io.liquidsoftware.common.logging.LoggerDelegate
import io.liquidsoftware.common.security.acl.AclChecker.Companion.ROLE_SYSTEM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Component

private fun getSystemAuthentication(): UsernamePasswordAuthenticationToken {
  val userDetails = UserDetailsWithId("SYSTEM",
    User("SYSTEM", "", listOf(SimpleGrantedAuthority(ROLE_SYSTEM))))
  return UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
}

suspend fun <T> runAsSuperUser(block: suspend () -> T): T {
  val originalAuthentication: Authentication? = SecurityContextHolder.getContext().authentication
  SecurityContextHolder.getContext().authentication = getSystemAuthentication()
  return block().also {
    SecurityContextHolder.getContext().authentication = originalAuthentication
  }
}

fun <T> runAsSuperUserBlocking(block: suspend () -> T): T =
  runBlocking(Dispatchers.Default + SecurityCoroutineContext()) {
    val originalAuthentication: Authentication? = SecurityContextHolder.getContext().authentication
    SecurityContextHolder.getContext().authentication = getSystemAuthentication()
    block().also {
      SecurityContextHolder.getContext().authentication = originalAuthentication
    }
  }

@Component
class ExecutionContext {
  val log by LoggerDelegate()

  companion object {
    const val ANONYMOUS_USER_ID = "u_anonymous"
  }

  fun getCurrentUser() =
    SecurityContextHolder.getContext().authentication
      .also { log.debug("The current principal: {}", it.principal) }
      .let { when (it) {
        is UsernamePasswordAuthenticationToken -> (it.principal as UserDetailsWithId)
        else -> throw IllegalStateException("Unexpected Authentication type")
      }}

  fun getUserAccessKeys(): List<String> =
    SecurityContextHolder.getContext().authentication
      .let { when (it) {
        is UsernamePasswordAuthenticationToken -> {
          val userId = (it.principal as UserDetailsWithId).id
          log.debug("The current principal: {}", it.principal)
          listOf(userId) + it.authorities.map { auth -> auth.authority }
        }
        else -> listOf(ANONYMOUS_USER_ID)
      }}
}

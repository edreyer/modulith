package io.liquidsoftware.common.security.spring

import io.liquidsoftware.common.security.ExecutionContext
import io.liquidsoftware.common.security.acl.AccessSubject
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

@Component
class ExecutionContextAuthenticationAccessSubjectResolver(
  private val executionContext: ExecutionContext,
) : AuthenticationAccessSubjectResolver {
  override fun resolve(authentication: Authentication?): AccessSubject =
    executionContext.getAccessSubject()
}

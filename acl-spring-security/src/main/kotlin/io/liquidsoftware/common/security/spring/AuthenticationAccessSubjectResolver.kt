package io.liquidsoftware.common.security.spring

import io.liquidsoftware.common.security.acl.AccessSubject
import org.springframework.security.core.Authentication

fun interface AuthenticationAccessSubjectResolver {
  fun resolve(authentication: Authentication?): AccessSubject
}

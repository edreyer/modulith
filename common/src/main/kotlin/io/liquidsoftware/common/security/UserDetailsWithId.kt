package io.liquidsoftware.common.security

import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails

data class UserDetailsWithId(
  val id: String,
  val user: User
) : UserDetails by user

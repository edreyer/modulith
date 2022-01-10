package io.liquidsoftware.common.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "security.jwt.token")
data class JwtProperties(
  var secretKey: String = "rzxlszyykpbgqcflzxsqcysyhljt",
  // validity in milliseconds
  var validityInMs: Long = 3600000 // 1h
)

package io.liquidsoftware.common.security

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.UNAUTHORIZED)
class UnauthorizedAccessException(msg: String) : RuntimeException(msg)

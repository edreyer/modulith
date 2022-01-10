package io.liquidsoftware.common.web

import io.liquidsoftware.common.ext.hasResponseStatus

interface ControllerSupport {

  fun <T> Result<T>.throwIfSpringError(): Result<T> {
    if (this.isFailure) {
      this.onFailure {
        if (it.hasResponseStatus()) throw it
      }
    }
    return this
  }
}

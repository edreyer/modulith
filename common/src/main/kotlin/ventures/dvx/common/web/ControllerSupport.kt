package ventures.dvx.common.web

import ventures.dvx.common.ext.hasResponseStatus

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

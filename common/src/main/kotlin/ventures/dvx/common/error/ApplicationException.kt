package ventures.dvx.common.error

class ApplicationException : RuntimeException {
  override val message: String
  constructor(message: String) : super(message) {
    this.message = message
  }
  constructor(message: String, throwable: Throwable) : super(message, throwable) {
    this.message = message
  }
}

package ventures.dvx.common.error

class NotFoundException: ApplicationException {
  constructor(message: String) : super(message)
  constructor(message: String, throwable: Throwable) : super(message, throwable)
}

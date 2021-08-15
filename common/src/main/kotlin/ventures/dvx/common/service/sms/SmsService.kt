package ventures.dvx.common.service.sms

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import ventures.dvx.common.logging.LoggerDelegate

class SmsException(
  override val message: String,
  override val cause: Throwable
) : RuntimeException(message, cause)


interface SmsService {
  fun sendMessage(msisdn: String, message: String)
}

@Component
@Profile("!prod")
class LoggingSmsService: SmsService {

  val log by LoggerDelegate()

  override fun sendMessage(msisdn: String, message: String) {
    log.info("SMS Message: msisdn=$msisdn, message=$message")
  }
}

@Component
@Profile("prod")
class SmsServiceImpl: SmsService {

  val log by LoggerDelegate()

  override fun sendMessage(msisdn: String, message: String) {
    log.info("SMS Message: msisdn=$msisdn, message=$message")
  }
}

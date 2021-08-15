package ventures.dvx.common.validation

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import org.springframework.stereotype.Component
import ventures.dvx.common.types.Msisdn
import ventures.dvx.common.types.getOrThrow

@Component
class MsisdnParser {

  companion object {
    private val phoneUtil = PhoneNumberUtil.getInstance()
  }

  fun isValid(input: String): Boolean = try {
    phoneUtil.parse(input, "US")
      .let { phoneUtil.isValidNumber(it) }
  } catch (e: NumberParseException) {
    false
  }

  @Throws(IllegalArgumentException::class)
  fun toInternational(input: String): Msisdn = try {
    phoneUtil.parse(input, "US")
      .let {
        require(phoneUtil.isValidNumber(it)) { "invalid phone number: $input" }
        Msisdn.of(phoneUtil.format(it, PhoneNumberUtil.PhoneNumberFormat.E164))
          .getOrThrow()
      }
  } catch (e: NumberParseException) {
    throw IllegalArgumentException("invalid phone number: $input")
  }

  @Throws(IllegalArgumentException::class)
  fun toInternational(input: Msisdn): Msisdn = toInternational(input.value)

}

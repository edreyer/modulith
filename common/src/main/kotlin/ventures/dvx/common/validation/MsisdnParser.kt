package ventures.dvx.common.validation

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil

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
  fun toInternational(input: String): String = try {
    phoneUtil.parse(input, "US")
      .let {
        require(phoneUtil.isValidNumber(it)) { "invalid phone number: $input" }
        phoneUtil.format(it, PhoneNumberUtil.PhoneNumberFormat.E164)
      }
  } catch (e: NumberParseException) {
    throw IllegalArgumentException("invalid phone number: $input")
  }

}

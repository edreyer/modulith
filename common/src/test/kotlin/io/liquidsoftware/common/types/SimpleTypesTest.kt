package io.liquidsoftware.common.types

import arrow.core.raise.either
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test


class SimpleTypesTest {

  @Test
  fun `Test NonEmptyString`() {
    either { NonEmptyString.of("good") }.isRight() shouldBe true
    either { NonEmptyString.of("") }.isRight() shouldBe false
  }

  @Test
  fun `Test EmailAddress`() {
    either { EmailAddress.of("erik@liquidsoftware.io") }.isRight() shouldBe true
    either { EmailAddress.of("erik@liquidsoftware") }.isRight() shouldBe false
    either { EmailAddress.of("") }.isRight() shouldBe false
  }

  @Test
  fun `Test PostalCode`() {
    either { PostalCode.of("12345") }.isRight() shouldBe true
    either { PostalCode.of("123") }.isRight() shouldBe false
    either { PostalCode.of("") }.isRight() shouldBe false
  }

  @Test
  fun `Test Msisdn`() {
    either { Msisdn.of("+15125551212") }.isRight() shouldBe true
    either { Msisdn.of("5125551212") }.isRight() shouldBe true
    either { Msisdn.of("5551212") }.isRight() shouldBe false
  }

}

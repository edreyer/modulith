package io.liquidsoftware.common.types

import arrow.core.continuations.effect
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test


class SimpleTypesTest {

  @Test
  fun `Test NonEmptyString`() = runBlocking {
    effect { NonEmptyString.of("good") }.toEither().isRight() shouldBe true
    effect { NonEmptyString.of("") }.toEither().isRight() shouldBe false
  }

  @Test
  fun `Test EmailAddress`() = runBlocking {
    effect { EmailAddress.of("erik@curbee.com") }.toEither().isRight() shouldBe true
    effect { EmailAddress.of("erik@curbee") }.toEither().isRight() shouldBe false
    effect { EmailAddress.of("") }.toEither().isRight() shouldBe false
  }

  @Test
  fun `Test PostalCode`() = runBlocking {
    effect { PostalCode.of("12345") }.toEither().isRight() shouldBe true
    effect { PostalCode.of("123") }.toEither().isRight() shouldBe false
    effect { PostalCode.of("") }.toEither().isRight() shouldBe false
  }

  @Test
  fun `Test Msisdn`() = runBlocking {
    effect { Msisdn.of("+15125551212") }.toEither().isRight() shouldBe true
    effect { Msisdn.of("5125551212") }.toEither().isRight() shouldBe true
    effect { Msisdn.of("5551212") }.toEither().isRight() shouldBe false
  }

}

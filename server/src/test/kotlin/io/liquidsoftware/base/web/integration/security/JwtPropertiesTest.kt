package io.liquidsoftware.base.web.integration.security

import io.liquidsoftware.base.server.MicrolithApplication
import io.liquidsoftware.common.security.JwtProperties
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

class JwtPropertiesTest {
  @Nested
  internal inner class UnitTest {
    private lateinit var properties: JwtProperties
    @BeforeEach
    private fun setup() {
      properties = JwtProperties()
    }

    @Test
    fun testProperties() {
      Assertions.assertThat(properties.secretKey).isEqualTo("rzxlszyykpbgqcflzxsqcysyhljt")
      Assertions.assertThat(properties.validityInMs).isEqualTo(3600000L)
    }
  }

  @Nested
  @SpringBootTest(classes = [MicrolithApplication::class])
  @TestPropertySource(properties = [
    "security.jwt.token.secretKey=testrzxlszyykpbgqcflzxsqcysyhljt",
    "security.jwt.token.validityInMs=100"
  ])
  internal inner class IntegrationTest {
    @Autowired
    private lateinit var properties: JwtProperties
    @Test
    fun testProperties() {
      Assertions.assertThat(properties.secretKey).isEqualTo("testrzxlszyykpbgqcflzxsqcysyhljt")
      Assertions.assertThat(properties.validityInMs).isEqualTo(100)
    }
  }
}
package io.liquidsoftware.base.web.integration.security

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.liquidsoftware.base.server.ModulithApplication
import io.liquidsoftware.common.security.JwtProperties
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
    fun setup() {
      properties = JwtProperties()
    }

    @Test
    fun testProperties() {
      assertThat(properties.secretKey).isEqualTo("rzxlszyykpbgqcflzxsqcysyhljt")
      assertThat(properties.validityInMs).isEqualTo(3600000L)
    }
  }

  @Nested
  @SpringBootTest(classes = [ModulithApplication::class])
  @TestPropertySource(properties = [
    "security.jwt.token.secretKey=testrzxlszyykpbgqcflzxsqcysyhljt",
    "security.jwt.token.validityInMs=100"
  ])
  internal inner class IntegrationTest {
    @Autowired
    private lateinit var properties: JwtProperties
    @Test
    fun testProperties() {
      assertThat(properties.secretKey).isEqualTo("testrzxlszyykpbgqcflzxsqcysyhljt")
      assertThat(properties.validityInMs).isEqualTo(100)
    }
  }
}

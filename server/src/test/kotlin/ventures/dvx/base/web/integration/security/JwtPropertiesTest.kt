package ventures.dvx.base.web.integration.security

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import ventures.dvx.common.security.JwtProperties

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
  @SpringBootTest
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

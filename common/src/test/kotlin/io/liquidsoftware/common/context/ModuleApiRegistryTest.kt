package io.liquidsoftware.common.context

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ModuleApiRegistryTest {

  @Test
  fun `register exposes api until destroyed`() {
    val registration = ModuleApiRegistry.register(SampleApi::class, SampleApi("first"))

    assertThat(ModuleApiRegistry.require(SampleApi::class).value).isEqualTo("first")

    registration.destroy()

    val error = assertThrows<IllegalStateException> {
      ModuleApiRegistry.require(SampleApi::class)
    }

    assertThat(error.message).isEqualTo("No module API registered for io.liquidsoftware.common.context.ModuleApiRegistryTest.SampleApi")
  }

  @Test
  fun `destroy does not remove newer replacement`() {
    val first = ModuleApiRegistry.register(SampleApi::class, SampleApi("first"))
    val second = ModuleApiRegistry.register(SampleApi::class, SampleApi("second"))

    first.destroy()

    assertThat(ModuleApiRegistry.require(SampleApi::class))
      .prop(SampleApi::value).isEqualTo("second")

    second.destroy()
  }

  private data class SampleApi(
    val value: String,
  )
}

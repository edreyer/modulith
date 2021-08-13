package ventures.dvx.common.mapping


import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ventures.dvx.common.mapping.DataClassMapper.Companion.setMapper

internal class DataClassMapperTest {
  data class FooIn(val name: String)
  data class BarIn(val name: String, val foos: Set<FooIn>)
  data class BazIn(val foo: FooIn)

  data class FooOut(val name: String)
  data class FooOut2(val name: String, val age: Int, val active: Boolean)
  data class BarOut(val name: String, val foos: Set<FooOut>)
  data class BazOut(val foo: FooOut)


  val fooMapper = DataClassMapper<FooIn, FooOut>()
  var fooWithTargetParamsMapper = DataClassMapper<FooIn, FooOut2>()
    .targetParameterSupplier("active") { true }
    .targetParameterSupplier(FooOut2::age) { 65 }

  val bazMapper = DataClassMapper<BazIn, BazOut>()
    .register("foo", fooMapper)

  val bazMapperAlternative = DataClassMapper<BazIn, BazOut>()
    .register(BazIn::foo, fooMapper)

  val barMapper = DataClassMapper<BarIn, BarOut>()
    .register("foos", setMapper(fooMapper))

  val barMapperAlternative = DataClassMapper<BarIn, BarOut>()
    .register(BarIn::foos, setMapper(fooMapper))

  @Test
  fun `simple mapping foo`() {
    assertThat(fooMapper(FooIn("kermit")))
      .isEqualTo(FooOut("kermit"))
  }

  @Test
  fun `simple mapping foo with default param in target`() {
    assertThat(fooWithTargetParamsMapper(FooIn("kermit")))
      .isEqualTo(FooOut2("kermit", 65, true))
  }

  @Test
  fun `transitive mapping baz(foo)`() {
    assertThat(bazMapper(BazIn(FooIn("piggy"))))
      .isEqualTo(bazMapperAlternative(BazIn(FooIn("piggy"))))
      .isEqualTo(BazOut(FooOut("piggy")))
  }

  @Test
  fun `set mapping bar`() {
    val inValue = BarIn("kermit", setOf(FooIn("piggy")))

    val outValue = barMapper(inValue)
    val outValue2 = barMapperAlternative(inValue)

    assertThat(outValue)
      .isEqualTo(outValue2)
      .isEqualTo(BarOut("kermit", setOf(FooOut("piggy"))))
  }
}

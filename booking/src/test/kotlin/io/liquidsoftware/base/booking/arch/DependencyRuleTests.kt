package io.liquidsoftware.base.booking.arch

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.liquidsoftware.arch.HexagonalArchitecture
import io.liquidsoftware.common.context.ModuleApiRegistry
import org.junit.jupiter.api.Test

class DependencyRuleTests {

  val module = "booking"

  @Test
  fun validateBookingContextArchitecture() {
    HexagonalArchitecture.boundedContext("io.liquidsoftware.base.$module")

      .withDomainLayer("domain")

      .withAdaptersLayer("adapter")
      .incoming("in.web")
      .outgoing("out.persistence")
      .outgoing("out.module")
      .and()

      .withApplicationLayer("application")
      .workflows("workflows")
      .incomingPorts("port.in")
      .outgoingPorts("port.out")
      .and()
      .withConfiguration("config")
      .check(
        ClassFileImporter()
          .importPackages("io.liquidsoftware.base.$module..")
      )
  }

  @Test
  fun testDomainPackageDependencies() {
    noClasses()
      .that()
      .resideInAPackage("io.liquidsoftware.base.$module.domain..")
      .should()
      .dependOnClassesThat()
      .resideInAnyPackage("io.liquidsoftware.base.$module.application..")
      .check(
        ClassFileImporter()
          .importPackages("io.liquidsoftware.base.$module..")
      )

    noClasses()
      .that()
      .resideInAPackage("io.liquidsoftware.base.$module.domain..")
      .should()
      .dependOnClassesThat()
      .resideInAnyPackage("io.liquidsoftware.base.$module.adapter..")
      .check(
        ClassFileImporter()
          .importPackages("io.liquidsoftware.base.$module..")
      )

  }

  @Test
  fun testWorkflowPackageDependencies() {
    noClasses()
      .that()
      .resideInAPackage("io.liquidsoftware.base.$module.application.workflows..")
      .should()
      .dependOnClassesThat()
      .resideInAnyPackage("io.liquidsoftware.base.$module.adapter..")
      .check(
        ClassFileImporter()
          .importPackages("io.liquidsoftware.base.$module..")
      )
  }

  @Test
  fun confineModuleApiRegistryToBoundaryAdaptersAndConfig() {
    noClasses()
      .that()
      .resideInAPackage("io.liquidsoftware.base.$module..")
      .and()
      .resideOutsideOfPackage("io.liquidsoftware.base.$module.arch..")
      .and()
      .resideOutsideOfPackages(
        "io.liquidsoftware.base.$module.adapter.out.module..",
        "io.liquidsoftware.base.$module.config.."
      )
      .should()
      .dependOnClassesThat()
      .haveFullyQualifiedName(ModuleApiRegistry::class.java.name)
      .check(
        ClassFileImporter()
          .importPackages("io.liquidsoftware.base.$module..")
      )
  }

}

package io.liquidsoftware.base.booking.api.arch

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

class DependencyRuleTests {

  @Test
  fun bookingApiMustNotDependOnLegacyWorkflowPackage() {
    noClasses()
      .that()
      .resideInAPackage("io.liquidsoftware.base.booking..")
      .should()
      .dependOnClassesThat()
      .resideInAnyPackage("io.liquidsoftware.common.workflow..")
      .check(
        ClassFileImporter()
          .importPackages("io.liquidsoftware.base.booking..")
      )
  }
}

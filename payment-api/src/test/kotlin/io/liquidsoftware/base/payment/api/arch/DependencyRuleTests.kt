package io.liquidsoftware.base.payment.api.arch

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

class DependencyRuleTests {

  @Test
  fun paymentApiMustNotDependOnLegacyWorkflowPackage() {
    noClasses()
      .that()
      .resideInAPackage("io.liquidsoftware.base.payment..")
      .should()
      .dependOnClassesThat()
      .resideInAnyPackage("io.liquidsoftware.common.workflow..")
      .check(
        ClassFileImporter()
          .importPackages("io.liquidsoftware.base.payment..")
      )
  }
}

package com.qros.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.qros", importOptions = ImportOption.DoNotIncludeTests.class)
class BackendDependencyRulesTest {
    private static final String MODULES = "com.qros.modules";
    private static final Pattern MODULE_CONTROLLER_PACKAGE =
            Pattern.compile("com\\.qros\\.modules\\.([^.]+)\\.controller(?:\\..*)?");
    private static final Pattern MODULE_REPOSITORY_PACKAGE =
            Pattern.compile("com\\.qros\\.modules\\.([^.]+)\\.repository(?:\\..*)?");

    @ArchTest
    void menu_must_not_depend_on_order(JavaClasses classes) {
        noClasses()
                .that().resideInAPackage("..modules.menu..")
                .should().dependOnClassesThat().resideInAPackage("..modules.order..")
                .because("menu is a catalog owner and must not know order workflows")
                .check(classes);
    }

    @ArchTest
    void table_must_not_depend_on_order(JavaClasses classes) {
        noClasses()
                .that().resideInAPackage("..modules.table..")
                .should().dependOnClassesThat().resideInAPackage("..modules.order..")
                .because("orders may use tables, but tables must stay independent from orders")
                .check(classes);
    }

    @ArchTest
    void kitchen_and_payment_must_not_depend_on_each_other(JavaClasses classes) {
        noClasses()
                .that().resideInAPackage("..modules.payment..")
                .should().dependOnClassesThat().resideInAPackage("..modules.kitchen..")
                .because("payment and kitchen workflows communicate through order state, not directly")
                .check(classes);

        noClasses()
                .that().resideInAPackage("..modules.kitchen..")
                .should().dependOnClassesThat().resideInAPackage("..modules.payment..")
                .because("payment and kitchen workflows communicate through order state, not directly")
                .check(classes);
    }

    @ArchTest
    void shared_must_not_depend_on_modules(JavaClasses classes) {
        noClasses()
                .that().resideInAPackage("..shared..")
                .should().dependOnClassesThat().resideInAPackage("..modules..")
                .because("shared is the lowest backend layer")
                .check(classes);
    }

    @ArchTest
    void controllers_must_not_depend_on_repositories_from_other_modules(JavaClasses classes) {
        noClasses()
                .that().resideInAPackage("..modules..controller..")
                .should(dependOnRepositoriesFromOtherModules())
                .because("controllers must talk to their own service boundary instead of another module repository")
                .check(classes);
    }

    private static ArchCondition<JavaClass> dependOnRepositoriesFromOtherModules() {
        return new ArchCondition<>("depend on repositories from other modules") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                String sourceModule = moduleFrom(item.getPackageName(), MODULE_CONTROLLER_PACKAGE);
                if (sourceModule == null) {
                    return;
                }

                for (Dependency dependency : item.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    String targetModule = moduleFrom(target.getPackageName(), MODULE_REPOSITORY_PACKAGE);
                    if (targetModule != null && !targetModule.equals(sourceModule)) {
                        events.add(SimpleConditionEvent.violated(item,
                                item.getName() + " depends on " + target.getName()));
                    }
                }
            }
        };
    }

    private static String moduleFrom(String packageName, Pattern pattern) {
        if (!packageName.startsWith(MODULES)) {
            return null;
        }

        Matcher matcher = pattern.matcher(packageName);
        return matcher.matches() ? matcher.group(1) : null;
    }
}

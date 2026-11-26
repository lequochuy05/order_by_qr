package com.qros.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@AnalyzeClasses(packages = "com.qros", importOptions = ImportOption.DoNotIncludeTests.class)
class BackendDependencyRulesTest {
    private static final String MODULES = "com.qros.modules";
    private static final Pattern MODULE_ANY_PACKAGE = Pattern.compile("com\\.qros\\.modules\\.([^.]+)(?:\\..*)?");
    private static final Pattern MODULE_REPOSITORY_PACKAGE =
            Pattern.compile("com\\.qros\\.modules\\.([^.]+)\\.repository(?:\\..*)?");

    @ArchTest
    void menu_must_not_depend_on_order(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAPackage("..modules.menu..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..modules.order..")
                .because("menu is a catalog owner and must not know order workflows")
                .check(classes);
    }

    @ArchTest
    void table_must_not_depend_on_order(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAPackage("..modules.table..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..modules.order..")
                .because("orders may use tables, but tables must stay independent from orders")
                .check(classes);
    }

    @ArchTest
    void kitchen_and_payment_must_not_depend_on_each_other(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAPackage("..modules.payment..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..modules.kitchen..")
                .because("payment and kitchen workflows communicate through order state, not directly")
                .check(classes);

        noClasses()
                .that()
                .resideInAPackage("..modules.kitchen..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..modules.payment..")
                .because("payment and kitchen workflows communicate through order state, not directly")
                .check(classes);
    }

    @ArchTest
    void payment_must_not_depend_on_order_repository_or_listener(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAPackage("..modules.payment..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..modules.order.repository..", "..modules.order.listener..")
                .because("payment should cross into order through the order service boundary")
                .check(classes);
    }

    @ArchTest
    void inventory_must_not_depend_on_order_repositories(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAPackage("..modules.inventory..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..modules.order.repository..")
                .because(
                        "order item loading belongs to the order workflow; inventory should receive loaded order items")
                .check(classes);
    }

    @ArchTest
    void shared_must_not_depend_on_higher_layers(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAPackage("com.qros.shared..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("com.qros.core..", "com.qros.infrastructure..", "com.qros.modules..")
                .because("shared is the lowest backend layer and must not depend on core, infrastructure, or modules")
                .check(classes);
    }

    @ArchTest
    void infrastructure_must_not_depend_on_core_or_modules(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAPackage("com.qros.infrastructure..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("com.qros.core..", "com.qros.modules..")
                .because("infrastructure adapters must stay decoupled from core configuration and domain modules")
                .check(classes);
    }

    @ArchTest
    void modules_must_not_depend_on_core(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAPackage("com.qros.modules..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("com.qros.core..")
                .because("domain modules must not depend on core bootstrap/security wiring")
                .check(classes);
    }

    @ArchTest
    void modules_must_be_free_of_cycles(JavaClasses classes) {
        slices().matching("com.qros.modules.(*)..")
                .should()
                .beFreeOfCycles()
                .because("domain modules in a modular monolith must form a directed acyclic graph")
                .check(classes);
    }

    @ArchTest
    void module_classes_must_not_depend_on_repositories_from_other_modules(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAPackage("..modules..")
                .should(dependOnRepositoriesFromOtherModules())
                .because("cross-module access must go through the owning module's service boundary, not its repository")
                .check(classes);
    }

    @ArchTest
    void spring_beans_must_not_have_constructor_dependency_cycles(JavaClasses classes) {
        List<JavaClass> beans = classes.stream()
                .filter(BackendDependencyRulesTest::isConcreteSpringBean)
                .sorted(Comparator.comparing(JavaClass::getName))
                .toList();

        Map<JavaClass, Set<JavaClass>> graph = new HashMap<>();
        for (JavaClass bean : beans) {
            Set<JavaClass> targets = new LinkedHashSet<>();
            Optional<JavaConstructor> injectionCtor = resolveInjectionConstructor(bean);
            if (injectionCtor.isPresent()) {
                for (JavaParameter param : injectionCtor.get().getParameters()) {
                    if (param.isAnnotatedWith(Lazy.class)) {
                        continue;
                    }
                    JavaClass targetType = unwrapInjectedType(param.getType());
                    if (targetType == null) {
                        continue;
                    }
                    for (JavaClass candidate : beans) {
                        if (!candidate.equals(bean) && candidate.isAssignableTo(targetType.getName())) {
                            targets.add(candidate);
                        }
                    }
                }
            }
            graph.put(bean, targets);
        }

        List<String> cycles = findBeanCycles(beans, graph);
        assertTrue(
                cycles.isEmpty(),
                () -> "Spring bean constructor dependency cycle(s) detected:\n" + String.join("\n", cycles));
    }

    private static boolean isConcreteSpringBean(JavaClass javaClass) {
        return !javaClass.isInterface()
                && !javaClass.getModifiers().contains(JavaModifier.ABSTRACT)
                && javaClass.isMetaAnnotatedWith(Component.class);
    }

    private static Optional<JavaConstructor> resolveInjectionConstructor(JavaClass bean) {
        Set<JavaConstructor> constructors = bean.getConstructors();
        Optional<JavaConstructor> autowired = constructors.stream()
                .filter(ctor -> ctor.isAnnotatedWith(Autowired.class))
                .findFirst();
        if (autowired.isPresent()) {
            return autowired;
        }
        if (constructors.size() == 1) {
            return Optional.of(constructors.iterator().next());
        }
        return Optional.empty();
    }

    private static JavaClass unwrapInjectedType(JavaType type) {
        JavaClass raw = type.toErasure();
        if (raw.isEquivalentTo(ObjectProvider.class)) {
            return null;
        }
        if (type instanceof JavaParameterizedType parameterized) {
            List<JavaType> args = parameterized.getActualTypeArguments();
            if ((raw.isEquivalentTo(List.class)
                            || raw.isEquivalentTo(Set.class)
                            || raw.isEquivalentTo(Collection.class)
                            || raw.isEquivalentTo(Optional.class))
                    && args.size() == 1) {
                return args.get(0).toErasure();
            }
            if (raw.isEquivalentTo(Map.class) && args.size() == 2) {
                return args.get(1).toErasure();
            }
        }
        return raw;
    }

    private static List<String> findBeanCycles(List<JavaClass> beans, Map<JavaClass, Set<JavaClass>> graph) {
        Set<JavaClass> visited = new HashSet<>();
        Set<JavaClass> visiting = new HashSet<>();
        List<JavaClass> path = new ArrayList<>();
        List<String> cycles = new ArrayList<>();

        for (JavaClass bean : beans) {
            if (!visited.contains(bean)) {
                dfsBeanCycles(bean, graph, visited, visiting, path, cycles);
            }
        }
        return cycles;
    }

    private static void dfsBeanCycles(
            JavaClass current,
            Map<JavaClass, Set<JavaClass>> graph,
            Set<JavaClass> visited,
            Set<JavaClass> visiting,
            List<JavaClass> path,
            List<String> cycles) {
        visiting.add(current);
        path.add(current);

        for (JavaClass next : graph.getOrDefault(current, Set.of())) {
            if (visiting.contains(next)) {
                int start = path.indexOf(next);
                List<JavaClass> cyclePath = new ArrayList<>(path.subList(start, path.size()));
                cyclePath.add(next);
                cycles.add(cyclePath.stream().map(JavaClass::getSimpleName).collect(Collectors.joining(" -> ")));
            } else if (!visited.contains(next)) {
                dfsBeanCycles(next, graph, visited, visiting, path, cycles);
            }
        }

        path.remove(path.size() - 1);
        visiting.remove(current);
        visited.add(current);
    }

    private static ArchCondition<JavaClass> dependOnRepositoriesFromOtherModules() {
        return new ArchCondition<>("depend on repositories from other modules") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                String sourceModule = moduleFrom(item.getPackageName(), MODULE_ANY_PACKAGE);
                if (sourceModule == null) {
                    return;
                }

                for (Dependency dependency : item.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    String targetModule = moduleFrom(target.getPackageName(), MODULE_REPOSITORY_PACKAGE);
                    if (targetModule != null && !targetModule.equals(sourceModule)) {
                        events.add(SimpleConditionEvent.violated(
                                item, item.getName() + " depends on " + target.getName()));
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

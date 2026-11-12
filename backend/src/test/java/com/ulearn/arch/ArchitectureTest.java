package com.ulearn.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import java.util.Set;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RestController;

/**
 * Encodes the modular-monolith structure from {@code design.md}: capabilities are independent
 * units under {@code com.ulearn}, reachable from one another only through their {@code api}
 * packages, with {@code common}/{@code config} sitting beneath all of them.
 */
@AnalyzeClasses(packages = "com.ulearn", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final Set<String> CAPABILITIES =
            Set.of("accounts", "catalog", "progress", "plans", "compilations", "sharing");

    private static final String[] CAPABILITY_PACKAGES =
            CAPABILITIES.stream().map(capability -> "com.ulearn." + capability + "..").toArray(String[]::new);

    private static String capabilityOf(JavaClass clazz) {
        String packageName = clazz.getPackageName();
        return CAPABILITIES.stream()
                .filter(capability -> packageName.equals("com.ulearn." + capability)
                        || packageName.startsWith("com.ulearn." + capability + "."))
                .findFirst()
                .orElse(null);
    }

    private static ArchCondition<JavaClass> onlyDependOnOtherCapabilitiesThroughTheirApi() {
        return new ArchCondition<>("only depend on other capabilities through their api packages") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                String ownCapability = capabilityOf(item);
                if (ownCapability == null) {
                    return;
                }
                for (Dependency dependency : item.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    String targetCapability = capabilityOf(target);
                    if (targetCapability == null || targetCapability.equals(ownCapability)) {
                        continue;
                    }
                    String apiPackage = "com.ulearn." + targetCapability + ".api";
                    if (!target.getPackageName().equals(apiPackage)
                            && !target.getPackageName().startsWith(apiPackage + ".")) {
                        events.add(SimpleConditionEvent.violated(
                                item,
                                item.getName() + " depends on " + target.getName() + ", which is inside the '"
                                        + targetCapability + "' capability; cross-capability access must go through "
                                        + apiPackage));
                    }
                }
            }
        };
    }

    @ArchTest
    static final ArchRule capabilities_are_reachable_only_through_their_api =
            classes().should(onlyDependOnOtherCapabilitiesThroughTheirApi());

    @ArchTest
    static final ArchRule common_and_config_do_not_depend_on_capabilities =
            noClasses()
                    .that()
                    .resideInAnyPackage("com.ulearn.common..", "com.ulearn.config..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(CAPABILITY_PACKAGES);

    @ArchTest
    static final ArchRule packages_are_free_of_cycles =
            slices().matching("com.ulearn.(*)..").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule controllers_do_not_access_repositories =
            noClasses()
                    .that()
                    .areAnnotatedWith(RestController.class)
                    .should()
                    .dependOnClassesThat()
                    .areAssignableTo(org.springframework.data.repository.Repository.class)
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule controllers_do_not_expose_entities =
            noClasses()
                    .that()
                    .areAnnotatedWith(RestController.class)
                    .should()
                    .dependOnClassesThat()
                    .areAnnotatedWith(Entity.class)
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule configuration_classes_live_in_config_packages =
            classes().that().areAnnotatedWith(Configuration.class).should().resideInAPackage("..config..");

    @ArchTest
    static final ArchRule rest_controllers_live_in_web_packages =
            classes()
                    .that()
                    .areAnnotatedWith(RestController.class)
                    .should()
                    .resideInAPackage("..web..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule repositories_live_in_persistence_packages =
            classes()
                    .that()
                    .areAnnotatedWith(Repository.class)
                    .should()
                    .resideInAPackage("..persistence..")
                    .allowEmptyShould(true);
}

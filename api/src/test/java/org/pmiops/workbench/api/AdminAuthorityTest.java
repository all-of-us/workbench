package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

public class AdminAuthorityTest {

  /**
   * API endpoints which are actually admin endpoints, but don't conform to the /admin/ infix
   * convention. Ideally these REST paths should eventually be updated for consistency. Avoid adding
   * more entries to this list and put /admin/ in the REST path instead.
   */
  private static final Set<String> NONCONFORMING_ADMIN_METHODS =
      ImmutableSet.of(
          "postStatusAlert",
          "publishWorkspace",
          "unpublishWorkspace",
          "getInstitution",
          "getInstitutions",
          "createInstitution",
          "deleteInstitution",
          "updateInstitution",
          "setInstitutionUserInstructions",
          "deleteInstitutionUserInstructions",
          "createAuthDomain",
          "updateUserDisabledStatus");

  /**
   * Conversely, methods which have an "/admin/" infix, but do not require an Authority. This should
   * be very exceptional, and should entail some additional method of authentication.
   *
   * <p>Please add a comment for any exceptions added to this list.
   */
  private static final Set<String> NONAUTHORITY_ADMIN_METHODS =
      ImmutableSet.of(
          // logEgressEvent requires an API key secret; it's a webhook which cannot use authority
          "logEgressEvent",
          // Not technically an admin endpoint, but only enabled in lower environments
          "unsafeSelfBypassAccessRequirements");

  /**
   * Heuristic test: if there's a REST endpoint that looks like an admin endpoint (based on the
   * URL), ensure that it has an @AuthorityRequired annotation. Assert the inverse as well.
   * Exceptions are enumerated in the above allowlists.
   *
   * <p>To achieve this, we use reflection over our API controller methods as well as the Swagger
   * generated interfaces. There are three relevant classes:
   *
   * <ul>
   *   <li>our source code controller implementation, which may or may not have @AuthorityRequired
   *   <li>the generated FooApiDelegate interface, which our controller implements
   *   <li>and the generated FooApi interface, which integrates with Spring, and contains an
   *       annotation @RequestMapping with request path information
   * </ul>
   *
   * <p>We walk these relationships to check our heuristic. The following preconditions must hold:
   *
   * <ul>
   *   <li>all API controllers live in the RW "api" Java package, and are annotated
   *       with @RestController
   *   <li>Swagger codegen uses the naming pattern of FooApiDelegate and FooApi
   *   <li>Swagger codegen generates identical method names for the Api interface and delegate (the
   *       operationId is used currently)
   * </ul>
   */
  @Test
  public void testAdminEndpointsHaveAuthority() throws Exception {
    List<Class<?>> controllers =
        findClassesInApiPackageWithAnnotation(RestController.class).stream()
            .filter(
                c ->
                    Arrays.stream(c.getInterfaces())
                        .map(i -> i.getName())
                        .anyMatch(n -> n.endsWith("ApiDelegate")))
            .collect(Collectors.toList());
    Map<String, Class<?>> generatedApis =
        findClassesInApiPackageWithAnnotation(Validated.class).stream()
            .collect(Collectors.toMap(Class::getName, Functions.identity()));

    assertThat(controllers).isNotEmpty();
    assertThat(generatedApis).isNotEmpty();
    assertThat(controllers.size()).isEqualTo(generatedApis.size());

    Map<Class<?>, Class<?>> controllerToApi = new HashMap<>();
    for (Class<?> controller : controllers) {
      Class<?> delegateInterface = controllerToDelegateInterface(controller);

      // Here we depend on the Swagger generated naming convention. Not ideal, but we do not have
      // annotations to work with directly.
      String apiName = delegateInterface.getName().replaceFirst("ApiDelegate", "Api");
      assertThat(generatedApis).containsKey(apiName);

      controllerToApi.put(controller, generatedApis.get(apiName));
    }

    for (Class<?> controller : controllers) {
      assertWithMessage(
              "could not find associated Swagger generated API interface for "
                  + controller.getName())
          .that(controllerToApi)
          .containsKey(controller);

      Class<?> generatedApi = controllerToApi.get(controller);
      List<Method> apiMethods =
          Stream.of(generatedApi.getMethods())
              .filter(m -> m.isAnnotationPresent(RequestMapping.class))
              .collect(Collectors.toList());
      assertThat(apiMethods).isNotEmpty();

      Set<String> delegateMethods =
          Arrays.stream(controllerToDelegateInterface(controller).getMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());
      Map<String, Method> controllerMethodsByName =
          Stream.of(controller.getMethods())
              .filter(m -> delegateMethods.contains(m.getName()))
              .collect(Collectors.toMap(Method::getName, Functions.identity()));
      for (Method m : apiMethods) {
        assertThat(controllerMethodsByName).containsKey(m.getName());
        Method controllerMethod = controllerMethodsByName.get(m.getName());

        RequestMapping mapping = m.getAnnotation(RequestMapping.class);
        Optional<String> adminPath =
            Arrays.stream(mapping.value()).filter(v -> v.contains("/admin/")).findAny();

        boolean likelyAdminEndpoint =
            adminPath.isPresent()
                || NONCONFORMING_ADMIN_METHODS.contains(controllerMethod.getName());
        if (NONAUTHORITY_ADMIN_METHODS.contains(controllerMethod.getName())) {
          assertWithMessage(
                  String.format(
                      "controller method '%s' is marked as a non-authority "
                          + "admin method, expected it not to have the @AuthorityRequired annotation",
                      controllerMethod.getName()))
              .that(controllerMethod.isAnnotationPresent(AuthorityRequired.class))
              .isFalse();
        } else if (controllerMethod.isAnnotationPresent(AuthorityRequired.class)) {
          assertWithMessage(
                  String.format(
                      "Update AdminAuthorityTest or fix REST path to be consistent\n"
                          + "controller method '%s' has @AuthorityRequired, but API does not look "
                          + "like an admin path: '%s'",
                      controllerMethod.getName(), Joiner.on(',').join(mapping.value())))
              .that(likelyAdminEndpoint)
              .isTrue();
        } else {
          assertWithMessage(
                  String.format(
                      "Likely missing @AuthorityRequired annotation!\n"
                          + "controller method '%s' lacks @AuthorityRequired, but endpoint '%s' "
                          + "appears to be an admin path",
                      controllerMethod.getName(), adminPath.orElse("")))
              .that(likelyAdminEndpoint)
              .isFalse();
        }
      }
    }
  }

  private Class<?> controllerToDelegateInterface(Class<?> controller) {
    List<Class<?>> delegateInterfaces =
        Arrays.stream(controller.getInterfaces())
            // Ideally we'd also filter by @Generated, but this annotation has source retention
            // (not runtime).
            .filter(i -> i.getName().endsWith("ApiDelegate"))
            // Overloading Java runtime error with Collectors.toList(); may be fixed in Java 11.
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    assertWithMessage(
            "expected to find exactly one delegate interface for controller: "
                + controller.getName())
        .that(delegateInterfaces)
        .hasSize(1);

    return delegateInterfaces.get(0);
  }

  private List<Class<?>> findClassesInApiPackageWithAnnotation(
      Class<? extends Annotation> annotationClass) throws IOException {
    return ClassPath.from(ClassLoader.getSystemClassLoader()).getAllClasses().stream()
        .filter(clazz -> clazz.getPackageName().equalsIgnoreCase("org.pmiops.workbench.api"))
        .map(ClassInfo::load)
        .filter(clazz -> clazz.isAnnotationPresent(annotationClass))
        .collect(Collectors.toList());
  }
}

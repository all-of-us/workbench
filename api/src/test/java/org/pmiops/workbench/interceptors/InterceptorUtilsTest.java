package org.pmiops.workbench.interceptors;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.api.ProfileApiController;
import org.pmiops.workbench.api.ProfileController;
import org.pmiops.workbench.api.UserAdminApiController;
import org.pmiops.workbench.api.UserAdminController;
import org.pmiops.workbench.model.Authority;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.method.HandlerMethod;

@Import(FakeClockConfiguration.class)
@SpringJUnitConfig
public class InterceptorUtilsTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private HandlerMethod handler;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;

  private CloudTaskInterceptor interceptor;

  @BeforeEach
  public void setUp() {
    interceptor = new CloudTaskInterceptor();
  }

  @Test
  public void testGetControllerMethod_withAuthorities() throws Exception {
    Class<?> apiControllerClass = UserAdminApiController.class;
    Class<?>[] parameterTypes = {};
    String methodName = "getAllUsers";
    Method method = apiControllerClass.getMethod(methodName, parameterTypes);
    UserAdminApiController userAdminApiController =
        new UserAdminApiController(mock(UserAdminController.class));
    HandlerMethod handlerMethod = new HandlerMethod(userAdminApiController, method);

    Method controllerMethod = InterceptorUtils.getControllerMethod(handlerMethod);
    assertThat(controllerMethod.getName()).isEqualTo(methodName);
    AuthorityRequired req = controllerMethod.getAnnotation(AuthorityRequired.class);
    assertThat(req.value()).asList().containsExactly(Authority.ACCESS_CONTROL_ADMIN);
  }

  @Test
  public void testGetControllerMethod_withoutAuthorities() throws Exception {
    Class<?> apiControllerClass = ProfileApiController.class;
    Class<?>[] parameterTypes = {};
    String methodName = "getMe";
    Method method = apiControllerClass.getMethod(methodName, parameterTypes);
    ProfileApiController profileApiController =
        new ProfileApiController(mock(ProfileController.class));
    HandlerMethod handlerMethod = new HandlerMethod(profileApiController, method);

    Method controllerMethod = InterceptorUtils.getControllerMethod(handlerMethod);
    assertThat(controllerMethod.getName()).isEqualTo(methodName);
    AuthorityRequired req = controllerMethod.getAnnotation(AuthorityRequired.class);
    assertThat(req).isNull();
  }

  class NoMatchingApiControllerClass {}

  @Test
  public void testGetControllerMethod_missingControllerException() throws Exception {

    Class<?> apiControllerClass = NoMatchingApiControllerClass.class;
    Class<?>[] parameterTypes = {};
    String methodName = "toString";
    Method method = apiControllerClass.getMethod(methodName, parameterTypes);
    ProfileApiController profileApiController =
        new ProfileApiController(mock(ProfileController.class));
    HandlerMethod handlerMethod = mock(HandlerMethod.class);

    when(handlerMethod.getBeanType()).thenReturn((Class) NoMatchingApiControllerClass.class);
    when(handlerMethod.getMethod()).thenReturn(method);

    Throwable exception =
        assertThrows(
            RuntimeException.class, () -> InterceptorUtils.getControllerMethod(handlerMethod));
    assertEquals(
        "Missing org.pmiops.workbench.interceptors.InterceptorUtilsTest$NoMatchingControllerClass by name derived from org.pmiops.workbench.interceptors.InterceptorUtilsTest$NoMatchingApiControllerClass.",
        exception.getMessage());
  }

  class MatchingApiControllerClass {
    public String getCactus() {
      return "Cactus";
    }
  }

  class MatchingControllerClass {}

  @Test
  public void testGetControllerMethod_missingMethodException() throws Exception {

    Class<?> apiControllerClass = MatchingApiControllerClass.class;
    Class<?>[] parameterTypes = {};
    String methodName = "getCactus";
    Method method = apiControllerClass.getMethod(methodName, parameterTypes);
    HandlerMethod handlerMethod = mock(HandlerMethod.class);

    when(handlerMethod.getBeanType()).thenReturn((Class) MatchingApiControllerClass.class);
    when(handlerMethod.getMethod()).thenReturn(method);

    Throwable exception =
        assertThrows(
            RuntimeException.class, () -> InterceptorUtils.getControllerMethod(handlerMethod));
    assertEquals(
        "java.lang.NoSuchMethodException: org.pmiops.workbench.interceptors.InterceptorUtilsTest$MatchingControllerClass.getCactus()",
        exception.getMessage());
  }
}

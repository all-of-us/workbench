package org.pmiops.workbench.interceptors

import com.google.common.collect.ImmutableMap
import java.lang.reflect.Method
import java.util.regex.Pattern
import org.springframework.web.method.HandlerMethod

object InterceptorUtils {

    private val apiImplMap = ImmutableMap.of(
            "org.pmiops.workbench.api.WorkspacesApiController",
            "org.pmiops.workbench.workspaces.WorkspacesController",
            "org.pmiops.workbench.api.BillingApiController",
            "org.pmiops.workbench.billing.BillingController")

    fun getControllerMethod(handlerMethod: HandlerMethod): Method {
        // There's no concise way to find out what class implements the delegate interface, so instead
        // depend on naming conventions. Essentially, this removes "Api" from the class name.
        // If this becomes a bottleneck, consider caching the class mapping, or copying annotations
        // from our implementation to the Swagger wrapper at startup (locally it takes <1ms).
        val apiControllerPattern = Pattern.compile("(.*\\.[^.]+)Api(Controller)")
        val apiControllerMethod = handlerMethod.method
        val apiControllerName = apiControllerMethod.declaringClass.name

        // The matcher assumes that all Controllers are within the same package as the generated
        // ApiController (api package)
        // The following code allows Controllers to be moved into other packages by specifying the
        // mapping in `apiImplMap`
        val controllerName: String
        if (apiImplMap.containsKey(apiControllerName)) {
            controllerName = apiImplMap[apiControllerName]
        } else {
            controllerName = apiControllerPattern.matcher(apiControllerName).replaceAll("$1$2")
        }

        val controllerClass: Class<*>
        try {
            controllerClass = Class.forName(controllerName)
        } catch (e: ClassNotFoundException) {
            throw RuntimeException(
                    "Missing $controllerName by name derived from $apiControllerName.", e)
        }

        try {
            return controllerClass.getMethod(
                    apiControllerMethod.name, *apiControllerMethod.parameterTypes)
        } catch (e: NoSuchMethodException) {
            throw RuntimeException(e)
        }

    }
}

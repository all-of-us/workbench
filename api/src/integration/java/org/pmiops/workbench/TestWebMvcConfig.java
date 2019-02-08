package org.pmiops.workbench;

import org.pmiops.workbench.config.WebMvcConfig;
import org.pmiops.workbench.config.WorkbenchEnvironment;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;

@TestConfiguration
public class TestWebMvcConfig extends WebMvcConfig {

    @Bean
    @Primary
    public WorkbenchEnvironment workbenchEnvironment() {
        return new WorkbenchEnvironment(true, "appId");
    }

    /**
     * The loading of the spring application context for ApplicationTest
     * was throwing a BeanCreationException: Scope 'request' is not active
     * for the current thread. Since WorkbenchConfig is request scoped using
     * javax.inject.Provider we have to provide a way for the ApplicationTest
     * to handle such scoping.
     * @return
     */
//    @Bean
    public static CustomScopeConfigurer customScopeConfigurer(){
        CustomScopeConfigurer scopeConfigurer = new CustomScopeConfigurer();

        HashMap<String, Object> scopes = new HashMap<>();
        scopes.put(WebApplicationContext.SCOPE_REQUEST, new SimpleThreadScope());
        scopeConfigurer.setScopes(scopes);

        return scopeConfigurer;
    }
}

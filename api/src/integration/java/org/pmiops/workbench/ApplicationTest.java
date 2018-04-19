package org.pmiops.workbench;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TestWebMvcConfig.class})
public class ApplicationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    public void contextLoads() throws Exception {
        //This loads all dao's that implement JPA repositories
        Map<String, Object> repoBeans = context.getBeansWithAnnotation(NoRepositoryBean.class);
        for (Map.Entry<String, Object> entry : repoBeans.entrySet()) {
            assertThat(entry.getValue()).isNotNull();
        }
        //This loads all @Service, @Controller, @Component and @Configuration annotations
        Map<String, Object> componentBeans = context.getBeansWithAnnotation(Component.class);
        for (Map.Entry<String, Object> entry : componentBeans.entrySet()) {
            assertThat(entry.getValue()).isNotNull();
        }
    }
}

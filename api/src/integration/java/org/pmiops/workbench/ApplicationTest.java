package org.pmiops.workbench;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TestWebMvcConfig.class})
public class ApplicationTest {

    @Autowired
    private ApplicationContext context;

    /**
     * Spring Boot provides a mechanism that will create a schema and load data into it.
     * It loads SQL from the standard root classpath locations: src/integration/resources/schema.sql
     * and src/integration/resources/data.sql, respectively. In addition, Spring Boot processes the schema-${platform}.sql
     * and data-${platform}.sql files (if present), where platform is the value of spring.datasource.platform.
     * This allows you to switch to database-specific scripts if necessary. For example, you might choose
     * to set it to the vendor name of the database (hsqldb, h2, oracle, mysql, postgresql, and so on).
     *
     * @throws Exception
     */
    @Test
    public void contextLoads() throws Exception {
        List<Object> beans = new ArrayList<>();
        //This loads all dao's that implement JPA repositories
        beans.addAll(context.getBeansWithAnnotation(NoRepositoryBean.class).values());
        //This loads all @Service, @Controller, @Component and @Configuration annotations
        beans.addAll(context.getBeansWithAnnotation(Component.class).values());
        for (Object object : beans) {
            System.out.println(object);
            assertThat(object).isNotNull();
        }
    }
}

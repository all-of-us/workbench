package org.pmiops.workbench;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * This test that all application injection is done properly. It loads all JPA repositories,
 * Services, Controllers, Components and Configurations.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class ApplicationTest {

  @Autowired private ApplicationContext context;

  /**
   * Spring Boot provides a mechanism that will create a schema and load data into it. It loads SQL
   * from the standard root classpath locations: src/integration/resources/schema.sql and
   * src/integration/resources/data.sql, respectively. In addition, Spring Boot processes the
   * schema-${platform}.sql and data-${platform}.sql files (if present), where platform is the value
   * of spring.datasource.platform. This allows you to switch to database-specific scripts if
   * necessary. For example, you might choose to set it to the vendor name of the database (hsqldb,
   * h2, oracle, mysql, postgresql, and so on).
   *
   * <p>Ignoring this test due to observed flakiness and false positives. RW-4806
   */
  @Ignore
  @Test
  public void contextLoads() {
    List<Object> beans = new ArrayList<>();
    // This loads all dao's that implement JPA repositories
    beans.addAll(context.getBeansWithAnnotation(NoRepositoryBean.class).values());
    // This loads all @Service, @Controller, @Component and @Configuration annotations
    beans.addAll(context.getBeansWithAnnotation(Component.class).values());
    for (Object object : beans) {
      assertThat(object).isNotNull();
    }
  }
}

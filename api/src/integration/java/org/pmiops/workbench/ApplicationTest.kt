package org.pmiops.workbench

import org.assertj.core.api.Assertions.assertThat

import java.util.ArrayList
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Component
import org.springframework.test.context.junit4.SpringRunner

/**
 * This test that all application injection is done properly. It loads all JPA repositories,
 * Services, Controllers, Components and Configurations.
 */
@RunWith(SpringRunner::class)
@SpringBootTest(classes = [TestWebMvcConfig::class])
class ApplicationTest {

    @Autowired
    private val context: ApplicationContext? = null

    /**
     * Spring Boot provides a mechanism that will create a schema and load data into it. It loads SQL
     * from the standard root classpath locations: src/integration/resources/schema.sql and
     * src/integration/resources/data.sql, respectively. In addition, Spring Boot processes the
     * schema-${platform}.sql and data-${platform}.sql files (if present), where platform is the value
     * of spring.datasource.platform. This allows you to switch to database-specific scripts if
     * necessary. For example, you might choose to set it to the vendor name of the database (hsqldb,
     * h2, oracle, mysql, postgresql, and so on).
     *
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun contextLoads() {
        val beans = ArrayList<Any>()
        // This loads all dao's that implement JPA repositories
        beans.addAll(context!!.getBeansWithAnnotation(NoRepositoryBean::class.java).values)
        // This loads all @Service, @Controller, @Component and @Configuration annotations
        beans.addAll(context.getBeansWithAnnotation(Component::class.java).values)
        for (`object` in beans) {
            println(`object`)
            assertThat<Any>(`object`).isNotNull()
        }
    }
}

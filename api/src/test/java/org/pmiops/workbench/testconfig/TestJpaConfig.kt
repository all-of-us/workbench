package org.pmiops.workbench.testconfig

import java.util.Properties
import javax.persistence.EntityManagerFactory
import javax.sql.DataSource
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.annotation.EnableTransactionManagement

@TestConfiguration
@EnableJpaRepositories(basePackages = ["org.pmiops.workbench.cdr", "org.pmiops.workbench.db"])
@EnableTransactionManagement
class TestJpaConfig {

    val jdbcTemplate: JdbcTemplate
        @Bean
        get() = JdbcTemplate(dataSource())

    val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
        @Bean
        get() = NamedParameterJdbcTemplate(dataSource())

    @Bean
    fun dataSource(): DataSource {
        val dataSource = DriverManagerDataSource()
        dataSource.setDriverClassName("org.h2.Driver")
        dataSource.url = "jdbc:h2:mem:db;DB_CLOSE_DELAY=-1"
        dataSource.username = "sa"
        dataSource.password = "sa"

        return dataSource
    }

    @Bean
    fun entityManagerFactory(): LocalContainerEntityManagerFactoryBean {
        val em = LocalContainerEntityManagerFactoryBean()
        em.dataSource = dataSource()
        em.setPackagesToScan(*arrayOf("org.pmiops.workbench.cdr", "org.pmiops.workbench.db"))
        em.jpaVendorAdapter = HibernateJpaVendorAdapter()
        em.setJpaProperties(additionalProperties())
        return em
    }

    @Bean
    fun transactionManager(entityManagerFactory: EntityManagerFactory): JpaTransactionManager {
        val transactionManager = JpaTransactionManager()
        transactionManager.entityManagerFactory = entityManagerFactory
        return transactionManager
    }

    fun additionalProperties(): Properties {
        val hibernateProperties = Properties()

        hibernateProperties.setProperty("hibernate.hbm2ddl.auto", "create-drop")
        hibernateProperties.setProperty(
                "hibernate.dialect", "org.pmiops.workbench.cdr.CommonTestDialect")
        hibernateProperties.setProperty("hibernate.show_sql", "true")

        return hibernateProperties
    }
}

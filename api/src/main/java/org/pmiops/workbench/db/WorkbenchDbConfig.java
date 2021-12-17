package org.pmiops.workbench.db;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Spring configuration for our workbench database. Uses the spring.datasource.* properties from
 * application.properties to configure the connection. Applies to the model and DAO objects under
 * this package.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaAuditing
@EnableJpaRepositories(
    entityManagerFactoryRef = "entityManagerFactory",
    transactionManagerRef = "transactionManager",
    basePackages = {"org.pmiops.workbench.db"})
public class WorkbenchDbConfig {

  @Primary
  @Bean(name = "dataSourceProperties")
  @ConfigurationProperties(prefix = "spring.datasource")
  public DataSourceProperties dataSourceProperties() {
    System.out.println("~~~~~~!!!!!!!");
    System.out.println("~~~~~~!!!!!!!");
    System.out.println("~~~~~~!!!!!!!");
    System.out.println("~~~~~~!!!!!!!");
    System.out.println("~~~~~~!!!!!!!");
    System.out.println("~~~~~~!!!!!!!");
    System.out.println("~~~~~~!!!!!!!");
    System.out.println(System.getProperties());
    System.out.println("~~~~~~!!!!!!!2222");
    System.out.println("~~~~~~!!!!!!!2222");
    System.out.println("~~~~~~!!!!!!!2222");
    System.out.println("~~~~~~!!!!!!!22222");
    System.out.println(System.getenv("DB_DRIVER"));
    System.out.println(System.getenv("DB_CONNECTION_STRING"));
    System.out.println(System.getenv("WORKBENCH_DB_USER"));
    System.out.println(System.getenv("WORKBENCH_DB_PASSWORD"));
    System.out.println(System.getenv("CDR_DB_CONNECTION_STRING"));
    System.out.println(System.getenv("CDR_DB_USER"));
    System.out.println(System.getenv("CDR_DB_PASSWORD"));
    return new DataSourceProperties();
  }

  @Primary
  @Bean(name = "dataSource")
  @ConfigurationProperties(prefix = "spring.datasource")
  public DataSource dataSource() {
    return dataSourceProperties().initializeDataSourceBuilder().build();
  }

  @Primary
  @Bean(name = "entityManagerFactory")
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(
      EntityManagerFactoryBuilder builder, @Qualifier("dataSource") DataSource dataSource) {
    return builder
        .dataSource(dataSource)
        .packages("org.pmiops.workbench.db")
        .persistenceUnit("workbench")
        .build();
  }

  @Primary
  @Bean(name = "transactionManager")
  public PlatformTransactionManager transactionManager(
      @Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory) {
    return new JpaTransactionManager(entityManagerFactory);
  }

  /**
   * The PoolConfiguration to use for all data sources (with modification). The primary connection
   * info should be overwritten if used (callers may mutate, so use prototype scope).
   */
  @Bean("poolConfiguration")
  @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  @ConfigurationProperties(prefix = "spring.datasource")
  public PoolConfiguration poolConfig() {
    return new PoolProperties();
  }
}

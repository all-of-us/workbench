package org.pmiops.workbench.db;

import com.zaxxer.hikari.HikariDataSource;
import java.util.logging.Logger;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
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
  private static final Logger log = Logger.getLogger(WorkbenchDbConfig.class.getName());

  final Params params;

  public WorkbenchDbConfig(Params params) {
    this.params = params;
  }

  @Primary
  @Bean(name = "dataSource")
  public DataSource dataSource() {
    // main database name is consistent across environments
    return new HikariDataSource(params.createConfig("workbench"));
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
  public PoolConfiguration poolConfig() {
    return new PoolProperties();
  }
}

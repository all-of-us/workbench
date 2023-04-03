package org.pmiops.workbench.cdr;

import java.util.logging.Logger;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    entityManagerFactoryRef = "cdrEntityManagerFactory",
    transactionManagerRef = "cdrTransactionManager",
    basePackages = {"org.pmiops.workbench.cdr"})
/**
 * Spring configuration for connecting to a database with private CDR metadata based on the context
 * of the current request. Applies to the model and DAO objects within this package.
 */
public class CdrDbConfig {
  private static final Logger log = Logger.getLogger(CdrDbConfig.class.getName());

  @Bean(name = "cdrEntityManagerFactory")
  public LocalContainerEntityManagerFactoryBean getCdrEntityManagerFactory(
      EntityManagerFactoryBuilder builder, @Qualifier("cdrDataSource") DataSource dataSource) {
    return builder
        .dataSource(dataSource)
        .packages("org.pmiops.workbench.cdr")
        .persistenceUnit("cdr")
        .build();
  }

  @Bean(name = "cdrTransactionManager")
  public PlatformTransactionManager cdrTransactionManager(
      @Qualifier("cdrEntityManagerFactory") EntityManagerFactory cdrEntityManagerFactory) {
    return new JpaTransactionManager(cdrEntityManagerFactory);
  }

  @Bean(name = "cdrPoolConfiguration")
  public PoolConfiguration poolConfig(DataSourceProperties dsProps) {
    PoolProperties poolProps = new PoolProperties();
    poolProps.setUrl(dsProps.getUrl());
    poolProps.setUsername(dsProps.getUsername());
    poolProps.setPassword(dsProps.getPassword());
    return poolProps;
  }
}

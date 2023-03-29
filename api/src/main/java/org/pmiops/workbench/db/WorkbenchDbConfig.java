package org.pmiops.workbench.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Optional;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
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
  @Bean(name = "dataSource")
  public DataSource dataSource() {
    HikariConfig config = new HikariConfig();
    Optional<String> dbHost = getEnv("DB_HOST");
    boolean connectViaAppEngine = !dbHost.isPresent();
    config.setDriverClassName("com.mysql.cj.jdbc.Driver");
    // database name is consistent across environments
    config.setJdbcUrl(
        String.format("jdbc:mysql://%s/workbench", connectViaAppEngine ? "" : dbHost.get()));
    config.setUsername("workbench"); // consistent across environments
    config.setPassword(getEnvRequired("WORKBENCH_DB_PASSWORD"));
    if (connectViaAppEngine) {
      config.addDataSourceProperty("socketFactory", "com.google.cloud.sql.mysql.SocketFactory");
      config.addDataSourceProperty("cloudSqlInstance", getEnvRequired("CLOUD_SQL_INSTANCE_NAME"));
    }
    config.addDataSourceProperty("useSSL", false);
    return new HikariDataSource(config);
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

  Optional<String> getEnv(String name) {
    return Optional.ofNullable(System.getenv(name)).map(s -> s.trim()).filter(s -> s != "");
  }

  String getEnvRequired(String name) {
    return getEnv(name).orElseThrow(() -> new IllegalStateException(name + " not defined"));
  }
}

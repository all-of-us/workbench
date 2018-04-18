package org.pmiops.workbench;

import com.google.common.cache.LoadingCache;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.pmiops.workbench.cdr.CdrDbConfig;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.CdrVersion;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

@Configuration
@EnableJpaRepositories(basePackages = { "org.pmiops.workbench.cdr", "org.pmiops.workbench.db" })
@EnableTransactionManagement
public class TestCdrDbConfig extends CdrDbConfig {

  @Service
  @Primary
  public static class CdrDataSource extends AbstractRoutingDataSource {

    private CdrVersion cdrVersion = new CdrVersion();

    public CdrDataSource() throws ExecutionException {
      cdrVersion.setCdrVersionId(1L);
      Map<Object, Object> cdrVersionDataSourceMap = new HashMap<>();
      cdrVersionDataSourceMap.put(cdrVersion.getCdrVersionId(), cdrDataSource());
      setTargetDataSources(cdrVersionDataSourceMap);
      afterPropertiesSet();
      System.out.println("here");
    }

    @Override
    protected Object determineCurrentLookupKey() {
      cdrVersion.setCdrVersionId(1L);
      return cdrVersion.getCdrVersionId();
    }

    @Bean("testCdrDataSource")
    @Qualifier("testCdrDataSource")
    public DataSource cdrDataSource() {
      final DriverManagerDataSource dataSource = new DriverManagerDataSource();
      dataSource.setDriverClassName("org.h2.Driver");
      dataSource.setUrl("jdbc:h2:mem:db;DB_CLOSE_DELAY=-1");
      dataSource.setUsername("sa");
      dataSource.setPassword("sa");

      return dataSource;
    }

    @Bean(name = "testCdrEntityManagerFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean getCdrEntityManagerFactory() {
      final LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
      em.setDataSource(cdrDataSource());
      em.setPackagesToScan(new String[] { "org.pmiops.workbench.cdr", "org.pmiops.workbench.db" });
      em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
      final Properties hibernateProperties = new Properties();

      hibernateProperties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
      hibernateProperties.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
      hibernateProperties.setProperty("hibernate.show_sql", "true");
      em.setJpaProperties(hibernateProperties);
      return em;
    }

    @Bean(name = "testCdrTransactionManager")
    @Primary
    public PlatformTransactionManager cdrTransactionManager(
      @Qualifier("testCdrEntityManagerFactory") EntityManagerFactory cdrEntityManagerFactory) {
      return new JpaTransactionManager(cdrEntityManagerFactory);
    }

    @Bean(name = "testCdrPoolConfiguration")
    @Primary
    @ConfigurationProperties(prefix = "cdr.datasource")
    public PoolConfiguration poolConfig() {
      return new PoolProperties();
    }
  }
}

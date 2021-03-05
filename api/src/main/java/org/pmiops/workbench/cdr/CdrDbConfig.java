package org.pmiops.workbench.cdr;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.apache.log4j.Logger;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.stereotype.Service;
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
  private static final Logger log = Logger.getLogger(CdrDbConfig.class);

  @Service
  public static class CdrDataSource extends AbstractRoutingDataSource {
    @Autowired
    public CdrDataSource(
        CdrVersionDao cdrVersionDao,
        @Qualifier("poolConfiguration") PoolConfiguration basePoolConfig,
        @Qualifier("cdrPoolConfiguration") PoolConfiguration cdrPoolConfig)
        throws ExecutionException {
      String dbUser = cdrPoolConfig.getUsername();
      String dbPassword = cdrPoolConfig.getPassword();
      String originalDbUrl = cdrPoolConfig.getUrl();

      // Build a map of CDR version ID -> DataSource for use later, based on all the entries in the
      // cdr_version table. Note that if new CDR versions are inserted, we need to restart the
      // server in order for it to be used.
      // TODO: find a way to make sure CDR versions aren't shown in the UI until they are in use by
      // all servers.
      Map<Object, Object> cdrVersionDataSourceMap = new HashMap<>();
      for (DbCdrVersion cdrVersion : cdrVersionDao.findAll()) {
        int slashIndex = originalDbUrl.lastIndexOf('/');
        String dbUrl =
            originalDbUrl.substring(0, slashIndex + 1)
                + cdrVersion.getCdrDbName()
                + "?useSSL=false";
        DataSource dataSource =
            DataSourceBuilder.create()
                .driverClassName(basePoolConfig.getDriverClassName())
                .username(dbUser)
                .password(dbPassword)
                .url(dbUrl)
                .build();
        if (dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource) {
          org.apache.tomcat.jdbc.pool.DataSource tomcatSource =
              (org.apache.tomcat.jdbc.pool.DataSource) dataSource;
          // A Tomcat DataSource implements PoolConfiguration, therefore these pool parameters can
          // normally be populated via @ConfigurationProperties. Since we are directly initializing
          // DataSources here without a hook to @ConfigurationProperties, we instead need to
          // explicitly initialize the pool parameters here. We override the primary connection
          // info, as the autowired PoolConfiguration is initialized from the same set of properties
          // as the workbench DB.
          PoolConfiguration cdrPool = new PoolProperties();
          BeanUtils.copyProperties(basePoolConfig, cdrPool);
          cdrPool.setUsername(dbUser);
          cdrPool.setPassword(dbPassword);
          cdrPool.setUrl(dbUrl);
          tomcatSource.setPoolProperties(cdrPool);

          // The Spring autowiring is a bit of a maze here, log something concrete which will allow
          // verification that the DB settings in application.properties are actually being loaded.
          log.info("using Tomcat pool for CDR data source, with minIdle: " + cdrPool.getMinIdle());
        } else {
          log.warn(
              "not using Tomcat pool or initializing pool configuration; "
                  + "this should only happen within tests");
        }
        cdrVersionDataSourceMap.put(cdrVersion.getCdrVersionId(), dataSource);
      }
      setTargetDataSources(cdrVersionDataSourceMap);
      afterPropertiesSet();
    }

    @Override
    protected Object determineCurrentLookupKey() {
      return CdrVersionContext.getCdrVersion().getCdrVersionId();
    }
  }

  @Bean("cdrDataSource")
  public DataSource cdrDataSource(CdrDataSource cdrDataSource) {
    return cdrDataSource;
  }

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
  @ConfigurationProperties(prefix = "cdr.datasource")
  public PoolConfiguration poolConfig() {
    return new PoolProperties();
  }
}

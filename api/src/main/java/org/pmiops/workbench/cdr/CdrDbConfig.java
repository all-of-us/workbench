package org.pmiops.workbench.cdr;

import com.google.common.cache.LoadingCache;
import org.pmiops.workbench.config.CacheSpringConfiguration;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    entityManagerFactoryRef = "cdrEntityManagerFactory",
    transactionManagerRef = "cdrTransactionManager",
    basePackages = { "org.pmiops.workbench.cdr" }
)
/**
 * Spring configuration for connecting to a database with private or public CDR metadata based
 * on the context of the current request. Applies to the model and DAO objects within this package.
 */
public class CdrDbConfig {

  private static final String DB_DRIVER_CLASS_NAME_KEY = "spring.datasource.driver-class-name";
  private static final String DB_URL_KEY = "spring.datasource.url";
  private static final String DB_USER_KEY = "spring.datasource.username";
  private static final String DB_PASSWORD_KEY = "spring.datasource.password";
  private static final String WORKBENCH_DB_USER = "workbench";

  @Service
  public static class CdrDataSource extends AbstractRoutingDataSource {

    private boolean finishedInitialization = false;

    private final Long defaultCdrVersionId;

    @Autowired
    public CdrDataSource(CdrVersionDao cdrVersionDao,
                         @Qualifier("configCache") LoadingCache<String, Object> configCache) throws ExecutionException {
      WorkbenchConfig workbenchConfig = CacheSpringConfiguration.lookupWorkbenchConfig(configCache);
      Map<String, String> envVariables = System.getenv();
      String dbDriverClassName = envVariables.get(DB_DRIVER_CLASS_NAME_KEY);
      String dbUser = envVariables.get(DB_USER_KEY);
      String dbPassword = envVariables.get(DB_PASSWORD_KEY);
      String originalDbUrl = envVariables.get(DB_URL_KEY);
      boolean isWorkbenchDbUser = isWorkbenchDbUser(dbUser);

      // Build a map of CDR version ID -> DataSource for use later, based on all the entries in the
      // cdr_version table. Note that if new CDR versions are inserted, we need to restart the
      // server in order for it to be used.
      // TODO: find a way to make sure CDR versions aren't shown in the UI until they are in use by
      // all servers.
      Map<Object, Object> cdrVersionDataSourceMap = new HashMap<>();
      Long cdrVersionId = null;
      for (CdrVersion cdrVersion : cdrVersionDao.findAll()) {
        if (cdrVersion.getName().equals(workbenchConfig.cdr.defaultCdrVersion)) {
          cdrVersionId = cdrVersion.getCdrVersionId();
        }
        // The database name used for a given CDR version is depends on whether this is the
        // workbench database user or data browser user.
        String dbName = isWorkbenchDbUser ? cdrVersion.getCdrDbName() : cdrVersion.getPublicDbName();
        int slashIndex = originalDbUrl.lastIndexOf('/');
        String dbUrl = originalDbUrl.substring(0, slashIndex + 1) + dbName;
        DataSource dataSource = DataSourceBuilder
            .create()
            .driverClassName(dbDriverClassName)
            .username(dbUser)
            .password(dbPassword)
            .url(dbUrl)
            .build();
        cdrVersionDataSourceMap.put(cdrVersion.getCdrVersionId(), dataSource);
      }
      this.defaultCdrVersionId = cdrVersionId;
      if (this.defaultCdrVersionId == null) {
        throw new ServerErrorException("Default CDR version not found!");
      }
      setTargetDataSources(cdrVersionDataSourceMap);
      afterPropertiesSet();
    }

    @EventListener
    public void handleContextRefresh(ContextRefreshedEvent event) {
      finishedInitialization = true;
    }

    @Override
    protected Object determineCurrentLookupKey() {
      CdrVersion cdrVersion = CdrVersionContext.getCdrVersion();
      if (cdrVersion == null) {
        if (finishedInitialization) {
          throw new ServerErrorException("No CDR version specified!");
        }
        // While Spring beans are being initialized, this method can be called
        // in the course of attempting to determine metadata about the data source.
        // Return the the default CDR version for configuring metadata.
        // After Spring beans are finished being initialized, init() will
        // be called and we will start requiring clients to specify a CDR version.
        return defaultCdrVersionId;
      }
      return cdrVersion.getCdrVersionId();
    }
  }

  /**
   * Returns true if the DB user is the workbench DB user (which has access to private data);
   * false if it isn't.
   */
  public static boolean isWorkbenchDbUser() {
    return isWorkbenchDbUser(System.getProperty(DB_USER_KEY));
  }

  private static boolean isWorkbenchDbUser(String dbUser) {
    return dbUser.equals(WORKBENCH_DB_USER);
  }

  @Bean("cdrDataSource")
  public DataSource getCdrDataSource(CdrDataSource cdrDataSource) {
    return cdrDataSource;
  }

  @Bean(name = "cdrEntityManagerFactory")
  public LocalContainerEntityManagerFactoryBean getCdrEntityManagerFactory(
      EntityManagerFactoryBuilder builder,
      @Qualifier("cdrDataSource") DataSource dataSource) {
    return
        builder
            .dataSource(dataSource)
            .packages("org.pmiops.workbench.cdr")
            .persistenceUnit("cdr")
            .build();
  }

  @Bean(name = "cdrTransactionManager")
  public PlatformTransactionManager barTransactionManager(
      @Qualifier("cdrEntityManagerFactory") EntityManagerFactory cdrEntityManagerFactory) {
    return new JpaTransactionManager(cdrEntityManagerFactory);
  }
}

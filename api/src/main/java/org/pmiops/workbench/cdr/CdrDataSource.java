package org.pmiops.workbench.cdr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;
import javax.sql.DataSource;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Service;

@Service("cdrDataSource")
public class CdrDataSource extends AbstractRoutingDataSource {

  private static final Logger log = Logger.getLogger(CdrDataSource.class.getName());

  private final PoolConfiguration basePoolConfig;
  private final PoolConfiguration cdrPoolConfig;
  private final EntityManagerFactory emFactory;

  CdrDataSource(
      @Qualifier("poolConfiguration") PoolConfiguration basePoolConfig,
      @Qualifier("cdrPoolConfiguration") PoolConfiguration cdrPoolConfig,
      // Using CdrDbConfig.cdrEntityManagerFactory would cause a circular dependency.
      @Qualifier("entityManagerFactory") EntityManagerFactory emFactory) {
    this.basePoolConfig = basePoolConfig;
    this.cdrPoolConfig = cdrPoolConfig;
    this.emFactory = emFactory;
    resetTargetDataSources();
  }

  List<DbCdrVersion> getCdrVersions() {
    EntityManager em = emFactory.createEntityManager();
    String jpql = "SELECT v FROM DbCdrVersion v";
    TypedQuery<DbCdrVersion> query = em.createQuery(jpql, DbCdrVersion.class);
    try {
      return query.getResultList();
    } catch (PersistenceException e) {
      // This emits "Table 'CDR_VERSION' not found" before throwing to this catch, so it is not
      // necessary to warn further.
      return new ArrayList<>();
    }
  }

  void resetTargetDataSources() {
    String dbUser = cdrPoolConfig.getUsername();
    String dbPassword = cdrPoolConfig.getPassword();
    String originalDbUrl = cdrPoolConfig.getUrl();

    // Build a map of CDR version ID -> DataSource for use later, based on all the entries in the
    // cdr_version table. Note that if new CDR versions are inserted, we need to restart the
    // server in order for it to be used.
    // TODO: find a way to make sure CDR versions aren't shown in the UI until they are in use by
    // all servers.
    Map<Object, Object> cdrVersionDataSourceMap = new HashMap<>();
    for (DbCdrVersion cdrVersion : getCdrVersions()) {
      int slashIndex = originalDbUrl.lastIndexOf('/');
      String dbUrl =
          originalDbUrl.substring(0, slashIndex + 1) + cdrVersion.getCdrDbName() + "?useSSL=false";
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
        log.warning(
            "not using Tomcat pool or initializing pool configuration; "
                + "this should only happen within tests");
      }

      cdrVersionDataSourceMap.put(cdrVersion.getCdrVersionId(), dataSource);
    }
    setTargetDataSources(cdrVersionDataSourceMap);
  }

  @Override
  protected Object determineCurrentLookupKey() {
    return CdrVersionContext.getCdrVersion().getCdrVersionId();
  }
}

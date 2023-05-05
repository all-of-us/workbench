package org.pmiops.workbench.cdr;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
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
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Service;

@Service("cdrDataSource")
public class CdrDataSource extends AbstractRoutingDataSource {

  private static final Logger log = Logger.getLogger(CdrDataSource.class.getName());

  private final DbParams params;
  private final PoolConfiguration basePoolConfig;
  private final PoolConfiguration cdrPoolConfig;
  private final EntityManagerFactory emFactory;

  CdrDataSource(
      DbParams params,
      @Qualifier("poolConfiguration") PoolConfiguration basePoolConfig,
      @Qualifier("cdrPoolConfiguration") PoolConfiguration cdrPoolConfig,
      // Using CdrDbConfig.cdrEntityManagerFactory would cause a circular dependency.
      @Qualifier("entityManagerFactory") EntityManagerFactory emFactory) {
    this.params = params;
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

  DataSource createCdrDs(String dbName) {
    return new HikariDataSource(params.createConfig(dbName));
  }

  void resetTargetDataSources() {
    // Build a map of CDR version ID -> DataSource for use later, based on all the entries in the
    // cdr_version table. Note that if new CDR versions are inserted, we need to restart the
    // server in order for it to be used.
    // TODO: find a way to make sure CDR versions aren't shown in the UI until they are in use by
    // all servers.
    Map<Object, Object> cdrVersionDataSourceMap = new HashMap<>();
    for (DbCdrVersion cdrVersion : getCdrVersions()) {
      try {
        DataSource dataSource = createCdrDs(cdrVersion.getCdrDbName());
        if (dataSource == null) {
          continue;
        }
        cdrVersionDataSourceMap.put(cdrVersion.getCdrVersionId(), dataSource);
      } catch (HikariPool.PoolInitializationException e) {
        // If this is caught, java.sql.SQLSyntaxErrorException: Unknown database 'name'
        // was thrown, so no need to repeat here.
      }
    }
    setTargetDataSources(cdrVersionDataSourceMap);
  }

  @Override
  protected Object determineCurrentLookupKey() {
    return CdrVersionContext.getCdrVersion().getCdrVersionId();
  }
}

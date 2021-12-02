package org.pmiops.workbench.cdr.dao;

import java.util.List;
import org.pmiops.workbench.cdr.model.DbDSLinking;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface DSLinkingDao extends CrudRepository<DbDSLinking, Long> {

  List<DbDSLinking> findByDomainAndDenormalizedNameInOrderById(
      @Param("domain") String domain, @Param("names") List<String> names);
}

package org.pmiops.workbench.cdr.dao;
import org.pmiops.workbench.cdr.model.DbDomain;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DbDomainDao extends CrudRepository<DbDomain, Long> {
    // TODO -- maybe add order by
    List<DbDomain> findAll();

    List<DbDomain> findByDbType(String db_type);

    List<DbDomain> findByDbTypeAndAndConceptIdNotNull(String db_type);
}

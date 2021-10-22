package org.pmiops.workbench.cdr.dao;

import java.util.List;
import org.pmiops.workbench.cdr.model.DbDomainCard;
import org.springframework.data.repository.CrudRepository;

public interface DomainCardDao extends CrudRepository<DbDomainCard, Long> {
  List<DbDomainCard> findByOrderById();
}

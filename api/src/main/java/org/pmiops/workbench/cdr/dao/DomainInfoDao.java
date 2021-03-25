package org.pmiops.workbench.cdr.dao;

import java.util.List;
import org.pmiops.workbench.cdr.model.DbDomainInfo;
import org.springframework.data.repository.CrudRepository;

public interface DomainInfoDao extends CrudRepository<DbDomainInfo, Long> {
  List<DbDomainInfo> findByOrderByDomainId();
}

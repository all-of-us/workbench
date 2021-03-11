package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.DbDSDataDictionary;
import org.springframework.data.repository.CrudRepository;

public interface DSDataDictionaryDao extends CrudRepository<DbDSDataDictionary, Long> {
  DbDSDataDictionary findDbDSDataDictionaryByFieldNameAndDomain(String field_name, String domain);
}

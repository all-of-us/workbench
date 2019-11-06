package org.pmiops.workbench.db.dao;

import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.DbDataDictionaryEntry;
import org.springframework.data.repository.CrudRepository;

public interface DataDictionaryEntryDao extends CrudRepository<DbDataDictionaryEntry, Long> {

  Optional<DbDataDictionaryEntry> findByRelevantOmopTableAndFieldNameAndCdrVersion(
      String relevantOmopTable, String fieldName, CdrVersion cdrVersion);

  List<DbDataDictionaryEntry> findByFieldNameAndCdrVersion(String fieldName, CdrVersion cdrVersion);
}

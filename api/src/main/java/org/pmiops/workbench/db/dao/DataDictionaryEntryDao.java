package org.pmiops.workbench.db.dao;

import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.model.CdrVersionEntity;
import org.pmiops.workbench.db.model.DataDictionaryEntry;
import org.springframework.data.repository.CrudRepository;

public interface DataDictionaryEntryDao extends CrudRepository<DataDictionaryEntry, Long> {

  Optional<DataDictionaryEntry> findByRelevantOmopTableAndFieldNameAndCdrVersion(
      String relevantOmopTable, String fieldName, CdrVersionEntity cdrVersionEntity);

  List<DataDictionaryEntry> findByFieldNameAndCdrVersion(String fieldName, CdrVersionEntity cdrVersionEntity);
}

package org.pmiops.workbench.dataset;

import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.DataDictionaryEntry;
import org.springframework.data.repository.CrudRepository;

public interface DataDictionaryEntryDao extends CrudRepository<DataDictionaryEntry, Long> {

  DataDictionaryEntry findByRelevantOmopTableAndFieldNameAndCdrVersion(
      String relevantOmopTable, String fieldName, CdrVersion cdrVersion);
}

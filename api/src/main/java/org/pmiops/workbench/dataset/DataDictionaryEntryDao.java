package org.pmiops.workbench.dataset;

import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.DataDictionaryEntry;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface DataDictionaryEntryDao extends CrudRepository<DataDictionaryEntry, Long> {

  DataDictionaryEntry findByRelevantOmopTableAndFieldNameAndCdrVersion(String relevantOmopTable, String fieldName, CdrVersion cdrVersion);
}

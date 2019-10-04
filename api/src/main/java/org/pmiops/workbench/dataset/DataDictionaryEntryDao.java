package org.pmiops.workbench.dataset;

import org.pmiops.workbench.db.model.DataDictionaryEntry;
import org.springframework.data.repository.CrudRepository;

public interface DataDictionaryEntryDao extends CrudRepository<DataDictionaryEntry, Long> {

}

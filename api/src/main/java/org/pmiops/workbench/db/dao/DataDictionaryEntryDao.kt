package org.pmiops.workbench.db.dao

import java.util.Optional
import org.pmiops.workbench.db.model.CdrVersion
import org.pmiops.workbench.db.model.DataDictionaryEntry
import org.springframework.data.repository.CrudRepository

interface DataDictionaryEntryDao : CrudRepository<DataDictionaryEntry, Long> {

    fun findByRelevantOmopTableAndFieldNameAndCdrVersion(
            relevantOmopTable: String, fieldName: String, cdrVersion: CdrVersion): Optional<DataDictionaryEntry>

    fun findByFieldNameAndCdrVersion(fieldName: String, cdrVersion: CdrVersion): List<DataDictionaryEntry>
}

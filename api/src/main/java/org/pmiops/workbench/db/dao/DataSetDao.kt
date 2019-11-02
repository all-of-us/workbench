package org.pmiops.workbench.db.dao

import org.pmiops.workbench.db.model.DataSet
import org.springframework.data.repository.CrudRepository

interface DataSetDao : CrudRepository<DataSet, Long> {
    fun findByWorkspaceIdAndInvalid(workspaceId: Long, invalid: Boolean): List<DataSet>

    fun findDataSetsByCohortIds(cohortId: Long): List<DataSet>

    fun findDataSetsByConceptSetIds(conceptId: Long): List<DataSet>

    fun findByWorkspaceId(workspaceId: Long): List<DataSet>
}

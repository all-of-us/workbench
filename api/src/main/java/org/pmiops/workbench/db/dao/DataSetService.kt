package org.pmiops.workbench.db.dao

import com.google.cloud.bigquery.QueryJobConfiguration
import java.sql.Timestamp
import org.pmiops.workbench.db.model.Cohort
import org.pmiops.workbench.db.model.ConceptSet
import org.pmiops.workbench.db.model.DataSet
import org.pmiops.workbench.db.model.DataSetValue
import org.pmiops.workbench.db.model.Workspace
import org.pmiops.workbench.model.DataSetRequest
import org.pmiops.workbench.model.KernelTypeEnum
import org.pmiops.workbench.model.PrePackagedConceptSetEnum
import org.springframework.stereotype.Service

@Service
interface DataSetService {
    fun saveDataSet(
            name: String,
            includesAllParticipants: Boolean?,
            description: String,
            workspaceId: Long,
            cohortIdList: List<Long>,
            conceptIdList: List<Long>,
            values: List<DataSetValue>,
            prePackagedConceptSetEnum: PrePackagedConceptSetEnum,
            creatorId: Long,
            creationTime: Timestamp): DataSet

    fun generateQueryJobConfigurationsByDomainName(
            dataSet: DataSetRequest): Map<String, QueryJobConfiguration>

    fun generateCodeCells(
            kernelTypeEnum: KernelTypeEnum,
            dataSetName: String,
            qualifier: String,
            queryJobConfigurationMap: Map<String, QueryJobConfiguration>): List<String>

    fun cloneDataSetToWorkspace(
            fromDataSet: DataSet, toWorkspace: Workspace, cohortIds: Set<Long>, conceptSetIds: Set<Long>): DataSet

    fun getDataSets(workspace: Workspace): List<DataSet>

    fun getConceptSetsForDataset(dataSet: DataSet): List<ConceptSet>

    fun getCohortsForDataset(dataSet: DataSet): List<Cohort>
}

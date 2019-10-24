package org.pmiops.workbench.cdr

import org.pmiops.workbench.db.model.CdrVersionEntity
import org.pmiops.workbench.model.ArchivalStatus
import org.pmiops.workbench.model.CdrVersion
import org.pmiops.workbench.model.DataAccessLevel

// Immutable data transfer object for CDR version
internal class ImmutableCdrVersion(
        val cdrVersionId: Long,
        val isDefault: Boolean,
        val name: String,
        val dataAccessLevelStorage: Short,
        val dataAccessLevelApi: DataAccessLevel,
        val archivalStatus: ArchivalStatus,
        val releaseNumber: Short,
        val bigQueryProject: String,
        val bigqueryDataset: String,
        val elasticIndexBaseName: String) {
    companion object {
        @JvmStatic
        fun fromEntity(e : CdrVersionEntity): ImmutableCdrVersion =
                ImmutableCdrVersion(
                        e.cdrVersionId,
                        e.isDefault,
                        e.name,
                        e.dataAccessLevel,
                        e.dataAccessLevelEnum,
                        e.archivalStatusEnum,
                        e.releaseNumber,
                        e.bigqueryProject,
                        e.bigqueryDataset,
                        e.elasticIndexBaseName)
        fun fromApiModel(e: CdrVersion) =
                ImmutableCdrVersion(
                        e.cdrVersionId,
                        e.isDefault,
                        e.name,
                        e.dataAccessLevel,
                        e.dataAccessLevelEnum,
                        e.archivalStatusEnum,
                        e.releaseNumber,
                        e.bigqueryProject,
                        e.bigqueryDataset,
                        e.elasticIndexBaseName
                )

    }

    fun toEntity() : CdrVersionEntity {
        var result: CdrVersionEntity = CdrVersionEntity();
        result.cdrVersionId = cdrVersionId;
        result.isDefault = isDefault;
        result.name = name;
        result.dataAccessLevel = dataAccessLevelStorage;
        result.dataAccessLevelEnum = dataAccessLevelApi;
        result.archivalStatusEnum = archivalStatus;
        result.releaseNumber = releaseNumber;
        result.bigqueryProject = bigQueryProject;
        result.elasticIndexBaseName = elasticIndexBaseName;
        return result;
    }

}


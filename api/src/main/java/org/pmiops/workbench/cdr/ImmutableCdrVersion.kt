package org.pmiops.workbench.cdr

import org.pmiops.workbench.model.ArchivalStatus
import org.pmiops.workbench.model.CdrVersion
import org.pmiops.workbench.model.DataAccessLevel
import java.sql.Timestamp

// Immutable data transfer object for CDR version
class ImmutableCdrVersion(
        val cdrVersionId: Long,
        val isDefault: Boolean,
        val name: String,
        val dataAccessLevelStorage: Short,
        val dataAccessLevelApi: DataAccessLevel,
        val archivalStatus: ArchivalStatus,
        val releaseNumber: Short,
        val bigQueryProject: String,
        val bigqueryDataset: String,
        val creationTime: Timestamp,
        val numParticipants: Int,
        val cdrDbName: String,
        val elasticIndexBaseName: String) {
    companion object {
        @JvmStatic
        fun fromEntity(e : org.pmiops.workbench.db.model.CdrVersion): ImmutableCdrVersion =
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
                        e.creationTime,
                        e.numParticipants,
                        e.cdrDbName,
                        e.elasticIndexBaseName)
    }

    // convert to Database Entity class
    fun toEntity() : org.pmiops.workbench.db.model.CdrVersion {
        var result: org.pmiops.workbench.db.model.CdrVersion = org.pmiops.workbench.db.model.CdrVersion();
        result.cdrVersionId = cdrVersionId;
        result.isDefault = isDefault;
        result.name = name;
        result.dataAccessLevel = dataAccessLevelStorage;
        result.dataAccessLevelEnum = dataAccessLevelApi;
        result.archivalStatusEnum = archivalStatus;
        result.releaseNumber = releaseNumber;
        result.bigqueryProject = bigQueryProject;
        result.bigqueryDataset = bigqueryDataset;
        result.creationTime = creationTime;
        result.numParticipants = numParticipants;
        result.cdrDbName = cdrDbName;
        result.elasticIndexBaseName = elasticIndexBaseName;
        return result; 
    }

    // conversion to generated client API type.
    fun toClientCdrVerrsion(): CdrVersion {
        val result = CdrVersion();
        result.cdrVersionId = cdrVersionId.toString();
        result.name = name;
        result.dataAccessLevel = dataAccessLevelApi;
        result.archivalStatus = archivalStatus;
        result.creationTime = creationTime.time;
        return result;
    }
}


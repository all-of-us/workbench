package org.pmiops.workbench.db.model

import java.sql.Timestamp
import java.util.stream.Collectors
import javax.persistence.CollectionTable
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.Table
import javax.persistence.Transient
import javax.persistence.Version
import org.pmiops.workbench.model.PrePackagedConceptSetEnum

@Entity
@Table(name = "data_set")
class DataSet {

    @get:Id
    @get:GeneratedValue(strategy = GenerationType.IDENTITY)
    @get:Column(name = "data_set_id")
    var dataSetId: Long = 0
    @get:Column(name = "workspace_id")
    var workspaceId: Long = 0
    @get:Column(name = "name")
    var name: String? = null
    @get:Version
    @get:Column(name = "version")
    var version: Int = 0
    @get:Column(name = "description")
    var description: String? = null
    @get:Column(name = "creator_id")
    var creatorId: Long = 0
    @get:Column(name = "creation_time")
    var creationTime: Timestamp? = null
    @get:Column(name = "last_modified_time")
    var lastModifiedTime: Timestamp? = null
    @get:Column(name = "invalid")
    var invalid: Boolean? = null
    @get:Column(name = "includes_all_participants")
    var includesAllParticipants: Boolean? = null
    @get:ElementCollection
    @get:CollectionTable(name = "data_set_concept_set", joinColumns = [JoinColumn(name = "data_set_id")])
    @get:Column(name = "concept_set_id")
    var conceptSetIds: List<Long>? = null
    @get:ElementCollection
    @get:CollectionTable(name = "data_set_cohort", joinColumns = [JoinColumn(name = "data_set_id")])
    @get:Column(name = "cohort_id")
    var cohortIds: List<Long>? = null
    @get:ElementCollection
    @get:CollectionTable(name = "data_set_values", joinColumns = [JoinColumn(name = "data_set_id")])
    @get:Column(name = "values")
    var values: List<DataSetValue>? = null
    @get:Column(name = "prePackagedConceptSet")
    var prePackagedConceptSet: Short = 0

    var prePackagedConceptSetEnum: PrePackagedConceptSetEnum
        @Transient
        get() = CommonStorageEnums.prePackageConceptSetsFromStorage(prePackagedConceptSet)
        set(domain) {
            this.prePackagedConceptSet = CommonStorageEnums.prePackageConceptSetsToStorage(domain)!!
        }

    constructor() {
        version = DataSet.INITIAL_VERSION
    }

    constructor(
            dataSetId: Long,
            workspaceId: Long,
            name: String,
            description: String,
            creatorId: Long,
            creationTime: Timestamp,
            invalid: Boolean?) {
        this.dataSetId = dataSetId
        this.workspaceId = workspaceId
        this.name = name
        this.version = DataSet.INITIAL_VERSION
        this.description = description
        this.creatorId = creatorId
        this.creationTime = creationTime
        this.invalid = invalid
    }

    constructor(dataSet: DataSet) {
        name = dataSet.name
        version = DataSet.INITIAL_VERSION
        description = dataSet.description
        invalid = dataSet.invalid
        includesAllParticipants = dataSet.includesAllParticipants
        values = dataSet.values!!.stream().map<DataSetValue>(Function<DataSetValue, DataSetValue> { DataSetValue(it) }).collect<List<DataSetValue>, Any>(Collectors.toList())
        prePackagedConceptSet = dataSet.prePackagedConceptSet
    }

    companion object {
        private val INITIAL_VERSION = 1
    }
}

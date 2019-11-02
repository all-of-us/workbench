package org.pmiops.workbench.db.model

import java.sql.Timestamp
import java.util.HashSet
import javax.persistence.CollectionTable
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.persistence.Transient
import javax.persistence.Version
import org.pmiops.workbench.model.Domain
import org.pmiops.workbench.model.Surveys

@Entity
@Table(name = "concept_set")
class ConceptSet {

    @get:Id
    @get:GeneratedValue(strategy = GenerationType.IDENTITY)
    @get:Column(name = "concept_set_id")
    var conceptSetId: Long = 0
    @get:Version
    @get:Column(name = "version")
    var version: Int = 0
    @get:Column(name = "name")
    var name: String? = null
    @get:Column(name = "domain")
    var domain: Short = 0
    @get:Column(name = "survey")
    var survey: Short? = null
    @get:Column(name = "description")
    var description: String? = null
    @get:Column(name = "workspace_id")
    var workspaceId: Long = 0
    @get:ManyToOne
    @get:JoinColumn(name = "creator_id")
    var creator: User? = null
    @get:Column(name = "creation_time")
    var creationTime: Timestamp? = null
    @get:Column(name = "last_modified_time")
    var lastModifiedTime: Timestamp? = null
    @get:Column(name = "participant_count")
    var participantCount: Int = 0
    @get:ElementCollection(fetch = FetchType.EAGER)
    @get:CollectionTable(name = "concept_set_concept_id", joinColumns = [JoinColumn(name = "concept_set_id")])
    @get:Column(name = "concept_id")
    var conceptIds: Set<Long> = HashSet()

    var domainEnum: Domain
        @Transient
        get() = CommonStorageEnums.domainFromStorage(domain)
        set(domain) {
            this.domain = CommonStorageEnums.domainToStorage(domain)!!
        }

    var surveysEnum: Surveys
        @Transient
        get() = CommonStorageEnums.surveysFromStorage(survey)
        set(survey) {
            this.survey = CommonStorageEnums.surveysToStorage(survey)
        }

    constructor() {
        version = ConceptSet.INITIAL_VERSION
    }

    constructor(cs: ConceptSet) {
        description = cs.description
        name = cs.name
        domain = cs.domain
        survey = cs.survey
        creator = cs.creator
        version = ConceptSet.INITIAL_VERSION
        workspaceId = cs.workspaceId
        creationTime = cs.creationTime
        lastModifiedTime = cs.lastModifiedTime
        participantCount = cs.participantCount
        conceptIds = HashSet(cs.conceptIds)
    }

    companion object {
        private val INITIAL_VERSION = 1
    }
}

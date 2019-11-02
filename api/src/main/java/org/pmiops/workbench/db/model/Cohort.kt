package org.pmiops.workbench.db.model

import java.sql.Timestamp
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.Lob
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.persistence.Version

@Entity
@Table(name = "cohort")
class Cohort {

    @get:Id
    @get:GeneratedValue(strategy = GenerationType.IDENTITY)
    @get:Column(name = "cohort_id")
    var cohortId: Long = 0
    @get:Version
    @get:Column(name = "version")
    var version: Int = 0
    @get:Column(name = "name")
    var name: String? = null
    @get:Column(name = "type")
    var type: String? = null
    @get:Column(name = "description")
    var description: String? = null
    @get:Column(name = "workspace_id")
    var workspaceId: Long = 0
    @get:Lob
    @get:Column(name = "criteria")
    var criteria: String? = null
    @get:ManyToOne
    @get:JoinColumn(name = "creator_id")
    var creator: User? = null
    @get:Column(name = "creation_time")
    var creationTime: Timestamp? = null
    @get:Column(name = "last_modified_time")
    var lastModifiedTime: Timestamp? = null
    private var cohortReviews: MutableSet<CohortReview>? = null

    constructor() {}

    constructor(c: Cohort) {
        criteria = c.criteria
        description = c.description
        name = c.name
        type = c.type
        creator = c.creator
        workspaceId = c.workspaceId
        creationTime = c.creationTime
        lastModifiedTime = c.lastModifiedTime
    }

    @OneToMany(mappedBy = "cohortId", orphanRemoval = true, cascade = [CascadeType.ALL])
    fun getCohortReviews(): Set<CohortReview>? {
        return cohortReviews
    }

    fun setCohortReviews(cohortReviews: MutableSet<CohortReview>) {
        if (this.cohortReviews == null) {
            this.cohortReviews = cohortReviews
            return
        }
        this.cohortReviews!!.addAll(cohortReviews)
    }

    fun addCohortReview(cohortReview: CohortReview) {
        this.cohortReviews!!.add(cohortReview)
    }
}

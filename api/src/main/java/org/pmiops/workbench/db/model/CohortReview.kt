package org.pmiops.workbench.db.model

import java.sql.Timestamp
import java.util.Objects
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.Lob
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.persistence.Transient
import javax.persistence.Version
import org.apache.commons.lang3.builder.ToStringBuilder
import org.pmiops.workbench.model.ReviewStatus

@Entity
@Table(name = "cohort_review")
class CohortReview {

    @get:Id
    @get:GeneratedValue(strategy = GenerationType.IDENTITY)
    @get:Column(name = "cohort_review_id")
    var cohortReviewId: Long = 0
    @get:Version
    @get:Column(name = "version")
    var version: Int = 0
    @get:Column(name = "cohort_id")
    var cohortId: Long = 0
    @get:Column(name = "cdr_version_id")
    var cdrVersionId: Long = 0
    @get:Column(name = "creation_time")
    var creationTime: Timestamp? = null
    @get:Lob
    @get:Column(name = "cohort_definition")
    var cohortDefinition: String? = null
    @get:Column(name = "cohort_name")
    var cohortName: String? = null
    @get:Column(name = "description")
    var description: String? = null
    @get:Column(name = "last_modified_time")
    var lastModifiedTime: Timestamp? = null
    @get:Column(name = "matched_participant_count")
    var matchedParticipantCount: Long = 0
    @get:Column(name = "review_size")
    var reviewSize: Long = 0
    @get:Column(name = "reviewed_count")
    var reviewedCount: Long = 0
    @get:Column(name = "review_status")
    var reviewStatus: Short? = null
    @get:ManyToOne
    @get:JoinColumn(name = "creator_id")
    var creator: User? = null

    var reviewStatusEnum: ReviewStatus
        @Transient
        get() = StorageEnums.reviewStatusFromStorage(reviewStatus)
        set(reviewStatus) {
            reviewStatus = StorageEnums.reviewStatusToStorage(reviewStatus)
        }

    fun cohortReviewId(cohortReviewId: Long): CohortReview {
        this.cohortReviewId = cohortReviewId
        return this
    }

    fun version(version: Int): CohortReview {
        this.version = version
        return this
    }

    fun cohortId(cohortId: Long): CohortReview {
        this.cohortId = cohortId
        return this
    }

    fun cdrVersionId(cdrVersionId: Long): CohortReview {
        this.cdrVersionId = cdrVersionId
        return this
    }

    fun cohortName(cohortName: String): CohortReview {
        this.cohortName = cohortName
        return this
    }

    fun description(description: String): CohortReview {
        this.description = description
        return this
    }

    fun cohortDefinition(cohortDefinition: String): CohortReview {
        this.cohortDefinition = cohortDefinition
        return this
    }

    fun creationTime(creationTime: Timestamp): CohortReview {
        this.creationTime = creationTime
        return this
    }

    fun lastModifiedTime(lastModifiedTime: Timestamp): CohortReview {
        this.lastModifiedTime = lastModifiedTime
        return this
    }

    fun matchedParticipantCount(matchedParticipantCount: Long): CohortReview {
        this.matchedParticipantCount = matchedParticipantCount
        return this
    }

    fun reviewSize(reviewSize: Long): CohortReview {
        this.reviewSize = reviewSize
        return this
    }

    fun incrementReviewedCount() {
        this.reviewedCount = reviewedCount + 1
    }

    fun reviewedCount(reviewedCount: Long): CohortReview {
        this.reviewedCount = reviewedCount
        return this
    }

    fun reviewStatus(reviewStatus: Short?): CohortReview {
        this.reviewStatus = reviewStatus
        return this
    }

    fun reviewStatusEnum(reviewStatus: ReviewStatus): CohortReview {
        return this.reviewStatus(StorageEnums.reviewStatusToStorage(reviewStatus))
    }

    fun creator(creator: User): CohortReview {
        this.creator = creator
        return this
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o == null || javaClass != o.javaClass) {
            return false
        }
        val that = o as CohortReview?
        return (version == that!!.version
                && cohortId == that.cohortId
                && cdrVersionId == that.cdrVersionId
                && matchedParticipantCount == that.matchedParticipantCount
                && reviewSize == that.reviewSize
                && reviewedCount == that.reviewedCount
                && creationTime == that.creationTime
                && cohortDefinition == that.cohortDefinition
                && cohortName == that.cohortName
                && description == that.description
                && lastModifiedTime == that.lastModifiedTime
                && reviewStatus == that.reviewStatus)
    }

    override fun hashCode(): Int {
        return Objects.hash(
                version,
                cohortId,
                cdrVersionId,
                creationTime,
                cohortDefinition,
                cohortName,
                description,
                lastModifiedTime,
                matchedParticipantCount,
                reviewSize,
                reviewedCount,
                reviewStatus)
    }

    override fun toString(): String {
        return ToStringBuilder(this)
                .append("cohortReviewId", cohortReviewId)
                .append("version", version)
                .append("cohortId", cohortId)
                .append("cdrVersionId", cdrVersionId)
                .append("creationTime", creationTime)
                .append("cohortDefinition", cohortDefinition)
                .append("cohortName", cohortName)
                .append("description", description)
                .append("lastModifiedTime", lastModifiedTime)
                .append("matchedParticipantCount", matchedParticipantCount)
                .append("reviewSize", reviewSize)
                .append("reviewedCount", reviewedCount)
                .append("reviewStatus", reviewStatus)
                .toString()
    }
}

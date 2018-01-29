package org.pmiops.workbench.db.model;

import javax.persistence.GenerationType;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.pmiops.workbench.model.ReviewStatus;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(name = "cohort_review")
public class CohortReview {

    private long cohortReviewId;
    private long cohortId;
    private long cdrVersionId;
    private Timestamp creationTime;
    private Timestamp lastModifiedTime;
    private long matchedParticipantCount;
    private long reviewSize;
    private long reviewedCount;
    private ReviewStatus reviewStatus;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cohort_review_id")
    public long getCohortReviewId() {
        return cohortReviewId;
    }

    public void setCohortReviewId(long cohortReviewId) {
        this.cohortReviewId = cohortReviewId;
    }

    public CohortReview cohortReviewId(long cohortReviewId) {
        this.cohortReviewId = cohortReviewId;
        return this;
    }

    @Column(name = "cohort_id")
    public long getCohortId() {
        return cohortId;
    }

    public void setCohortId(long cohortId) {
        this.cohortId = cohortId;
    }

    public CohortReview cohortId(long cohortId) {
        this.cohortId = cohortId;
        return this;
    }

    @Column(name = "cdr_version_id")
    public long getCdrVersionId() {
        return cdrVersionId;
    }

    public void setCdrVersionId(long cdrVersionId) {
        this.cdrVersionId = cdrVersionId;
    }

    public CohortReview cdrVersionId(long cdrVersionId) {
        this.cdrVersionId = cdrVersionId;
        return this;
    }

    @Column(name = "creation_time")
    public Timestamp getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Timestamp creationTime) {
        this.creationTime = creationTime;
    }

    public CohortReview creationTime(Timestamp creationTime) {
        this.creationTime = creationTime;
        return this;
    }

    @Column(name = "last_modified_time")
    public Timestamp getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(Timestamp lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    public CohortReview lastModifiedTime(Timestamp lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
        return this;
    }

    @Column(name = "matched_participant_count")
    public long getMatchedParticipantCount() {
        return matchedParticipantCount;
    }

    public void setMatchedParticipantCount(long matchedParticipantCount) {
        this.matchedParticipantCount = matchedParticipantCount;
    }

    public CohortReview matchedParticipantCount(long matchedParticipantCount) {
        this.matchedParticipantCount = matchedParticipantCount;
        return this;
    }

    @Column(name = "review_size")
    public long getReviewSize() {
        return reviewSize;
    }

    public void setReviewSize(long reviewSize) {
        this.reviewSize = reviewSize;
    }

    public CohortReview reviewSize(long reviewSize) {
        this.reviewSize = reviewSize;
        return this;
    }

    public void incrementReviewedCount() {
        this.reviewedCount = reviewedCount + 1;
    }

    @Column(name = "reviewed_count")
    public long getReviewedCount() {
        return reviewedCount;
    }

    public void setReviewedCount(long reviewedCount) {
        this.reviewedCount = reviewedCount;
    }

    public CohortReview reviewedCount(long reviewedCount) {
        this.reviewedCount = reviewedCount;
        return this;
    }

    @Column(name = "review_status")
    public ReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(ReviewStatus reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public CohortReview reviewStatus(ReviewStatus reviewStatus) {
        this.reviewStatus = reviewStatus;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CohortReview that = (CohortReview) o;
        return cohortId == that.cohortId &&
                cdrVersionId == that.cdrVersionId &&
                matchedParticipantCount == that.matchedParticipantCount &&
                reviewSize == that.reviewSize &&
                reviewedCount == that.reviewedCount &&
                Objects.equals(lastModifiedTime, that.lastModifiedTime) &&
                reviewStatus == that.reviewStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cdrVersionId, lastModifiedTime, matchedParticipantCount, reviewSize, reviewedCount, reviewStatus);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("cohortReviewId", cohortReviewId)
                .append("cohortId", cohortId)
                .append("cdrVersionId", cdrVersionId)
                .append("creationTime", creationTime)
                .append("lastModifiedTime", lastModifiedTime)
                .append("matchedParticipantCount", matchedParticipantCount)
                .append("reviewSize", reviewSize)
                .append("reviewedCount", reviewedCount)
                .append("reviewStatus", reviewStatus)
                .toString();
    }
}

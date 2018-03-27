package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.ParticipantCohortAnnotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ParticipantCohortAnnotationDao extends JpaRepository<ParticipantCohortAnnotation, Long> {

    // Important: Keep in sync with all DB rows that should be copied.
    static final String ALIAS_ALL_COLUMNS_EXCEPT_REVIEW_ID =
            "cad.cohort_annotation_definition_id, pca.participant_id, pca.annotation_value_string, " +
                    "pca.annotation_value_integer, pca.annotation_value_date, " +
                    "caev.cohort_annotation_enum_value_id, pca.annotation_value_boolean";

    static final String ALL_COLUMNS_EXCEPT_REVIEW_ID =
            "cohort_annotation_definition_id, participant_id, annotation_value_string, " +
                    "annotation_value_integer, annotation_value_date, " +
                    "cohort_annotation_enum_value_id, annotation_value_boolean";

    ParticipantCohortAnnotation findByCohortReviewIdAndCohortAnnotationDefinitionIdAndParticipantId(
            @Param("cohortReviewId") long cohortReviewId,
            @Param("cohortAnnotationDefinitionId") long cohortAnnotationDefinitionId,
            @Param("participantId") long participantId);

    ParticipantCohortAnnotation findByAnnotationIdAndCohortReviewIdAndParticipantId(
            @Param("annotationId") long annotationId,
            @Param("cohortReviewId") long cohortReviewId,
            @Param("participantId") long participantId);

    List<ParticipantCohortAnnotation> findByCohortReviewIdAndParticipantId(
            @Param("cohortReviewId") long cohortReviewId,
            @Param("participantId") long participantId);

    // We use native SQL here as there may be a large number of rows within a
    // given cohort review; this avoids loading them into memory.
    @Modifying
    @Query(
        value = "INSERT INTO participant_cohort_annotations" +
        " (cohort_review_id, " + ALL_COLUMNS_EXCEPT_REVIEW_ID + ")" +
        " SELECT :toCohortReviewId, " + ALIAS_ALL_COLUMNS_EXCEPT_REVIEW_ID +
        " FROM participant_cohort_annotations pca" +
        " JOIN cohort_annotation_definition cad on :toCohortId = cad.cohort_id" +
        " JOIN cohort_annotation_enum_value caev on cad.cohort_annotation_definition_id = caev.cohort_annotation_definition_id" +
        " WHERE pca.cohort_review_id = :fromCohortReviewId",
        nativeQuery = true)
    void bulkCopyByCohortReviewAndCohort(
            @Param("fromCohortReviewId") long fromCohortReviewId,
            @Param("toCohortReviewId") long toCohortReviewId,
            @Param("toCohortId") long toCohortId);
}

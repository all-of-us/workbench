package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.ParticipantCohortAnnotation;
import org.pmiops.workbench.model.AnnotationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ParticipantCohortAnnotationDao extends JpaRepository<ParticipantCohortAnnotation, Long> {

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
        " (cohort_review_id, cohort_annotation_definition_id, participant_id, annotation_value_string," +
        " annotation_value_integer, annotation_value_date, cohort_annotation_enum_value_id, annotation_value_boolean)" +
        " SELECT :toCohortReviewId, new.cohort_annotation_definition_id, participant_id, annotation_value_string," +
        " annotation_value_integer, annotation_value_date, cohort_annotation_enum_value_id, annotation_value_boolean" +
        " FROM cohort_annotation_definition new" +
        " JOIN (SELECT column_name, participant_id, annotation_value_string, annotation_value_integer, annotation_value_date, " +
        "       annotation_value_boolean, enum_order" +
        "       FROM cohort_annotation_definition cad" +
        "       LEFT JOIN cohort_annotation_enum_value caev" +
        "       ON (cad.cohort_annotation_definition_id = caev.cohort_annotation_definition_id)" +
        "       JOIN participant_cohort_annotations pca " +
        "       ON (pca.cohort_annotation_definition_id = cad.cohort_annotation_definition_id" +
        "           AND pca.cohort_annotation_enum_value_id = caev.cohort_annotation_enum_value_id" +
        "           AND cohort_id = :fromCohortId" +
        "           AND annotation_type = :annotationType" +
        "           AND cohort_review_id = :fromCohortReviewId)" +
        ") AS old ON new.column_name = old.column_name" +
        " JOIN cohort_annotation_enum_value caev" +
        " ON (caev.cohort_annotation_definition_id = new.cohort_annotation_definition_id" +
        "     AND caev.enum_order = old.enum_order)" +
        " WHERE cohort_id = :toCohortId",
        nativeQuery = true)
    void bulkCopyEnumAnnotationsByCohortReviewAndCohort(
            @Param("fromCohortReviewId") long fromCohortReviewId,
            @Param("toCohortReviewId") long toCohortReviewId,
            @Param("fromCohortId") long fromCohortId,
            @Param("toCohortId") long toCohortId,
            @Param("annotationType") int annotationType);

    // We use native SQL here as there may be a large number of rows within a
    // given cohort review; this avoids loading them into memory.
    @Modifying
    @Query(
        value = "INSERT INTO participant_cohort_annotations" +
        " (cohort_review_id, cohort_annotation_definition_id, participant_id, annotation_value_string," +
        " annotation_value_integer, annotation_value_date, cohort_annotation_enum_value_id, annotation_value_boolean)" +
        " SELECT :toCohortReviewId, cohort_annotation_definition_id, participant_id, annotation_value_string," +
        " annotation_value_integer, annotation_value_date, cohort_annotation_enum_value_id, annotation_value_boolean" +
        " FROM cohort_annotation_definition new" +
        " JOIN (SELECT column_name, participant_id, annotation_value_string, annotation_value_integer," +
        "       annotation_value_date, cohort_annotation_enum_value_id, annotation_value_boolean" +
        "       FROM cohort_annotation_definition cad" +
        "       JOIN participant_cohort_annotations pca" +
        "       ON (cad.cohort_annotation_definition_id = pca.cohort_annotation_definition_id" +
        "           AND cohort_id = :fromCohortId" +
        "           AND annotation_type != :annotationType" +
        "           AND cohort_review_id = :fromCohortReviewId)" +
        ") AS old ON new.column_name = old.column_name" +
        " AND cohort_id = :toCohortId",
        nativeQuery = true)
    void bulkCopyNotEnumAnnotationsByCohortReviewAndCohort(
            @Param("fromCohortReviewId") long fromCohortReviewId,
            @Param("toCohortReviewId") long toCohortReviewId,
            @Param("fromCohortId") long fromCohortId,
            @Param("toCohortId") long toCohortId,
            @Param("annotationType") int annotationType);
}

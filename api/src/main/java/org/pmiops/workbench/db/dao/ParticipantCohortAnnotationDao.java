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
        " SELECT :toCohortReviewId, cad1.cohort_annotation_definition_id, participant_id, annotation_value_string," +
        " annotation_value_integer, annotation_value_date, caev1.cohort_annotation_enum_value_id, annotation_value_boolean" +
        " FROM cohort_annotation_definition cad" +
        " JOIN cohort_annotation_enum_value caev ON (cad.cohort_annotation_definition_id = caev.cohort_annotation_definition_id)" +
        " JOIN participant_cohort_annotations pca ON (pca.cohort_annotation_definition_id = cad.cohort_annotation_definition_id" +
        "                                            AND pca.cohort_annotation_enum_value_id = caev.cohort_annotation_enum_value_id" +
        "                                            AND cad.cohort_id = :fromCohortId" +
        "                                            AND cohort_review_id = :fromCohortReviewId)" +
        " JOIN cohort_annotation_definition cad1 ON (cad1.column_name = cad.column_name and cad1.cohort_id = :toCohortId)" +
        " JOIN cohort_annotation_enum_value caev1 ON (caev1.cohort_annotation_definition_id = cad1.cohort_annotation_definition_id" +
        "                                            AND caev1.enum_order = caev.enum_order)",
        nativeQuery = true)
    void bulkCopyEnumAnnotationsByCohortReviewAndCohort(@Param("fromCohortReviewId") long fromCohortReviewId,
                                                        @Param("toCohortReviewId") long toCohortReviewId,
                                                        @Param("fromCohortId") long fromCohortId,
                                                        @Param("toCohortId") long toCohortId);

    // We use native SQL here as there may be a large number of rows within a
    // given cohort review; this avoids loading them into memory.
    @Modifying
    @Query(
        value = "INSERT INTO participant_cohort_annotations" +
        " (cohort_review_id, cohort_annotation_definition_id, participant_id, annotation_value_string," +
        " annotation_value_integer, annotation_value_date, cohort_annotation_enum_value_id, annotation_value_boolean)" +
        " SELECT :toCohortReviewId, cad1.cohort_annotation_definition_id, participant_id, annotation_value_string," +
        " annotation_value_integer, annotation_value_date, cohort_annotation_enum_value_id, annotation_value_boolean" +
        " FROM cohort_annotation_definition cad" +
        " JOIN participant_cohort_annotations pca ON (cad.cohort_annotation_definition_id = pca.cohort_annotation_definition_id" +
        "                                            AND cad.cohort_id = :fromCohortId" +
        "                                            AND cohort_review_id = :fromCohortReviewId)" +
        " JOIN cohort_annotation_definition cad1 ON (cad.column_name = cad1.column_name AND cad1.cohort_id = :toCohortId)" +
        " WHERE NOT EXISTS" +
        " (SELECT 'x' FROM cohort_annotation_enum_value caev WHERE cad.cohort_annotation_definition_id = caev.cohort_annotation_definition_id)",
        nativeQuery = true)
    void bulkCopyNonEnumAnnotationsByCohortReviewAndCohort1(@Param("fromCohortReviewId") long fromCohortReviewId,
                                                            @Param("toCohortReviewId") long toCohortReviewId,
                                                            @Param("fromCohortId") long fromCohortId,
                                                            @Param("toCohortId") long toCohortId);
}

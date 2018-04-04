package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.ParticipantCohortAnnotation;
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
        " (cohort_review_id, cohort_annotation_definition_id, participant_id, cohort_annotation_enum_value_id)" +
        " SELECT :toCohortReviewId, toCad.cohort_annotation_definition_id, participant_id, toCaev.cohort_annotation_enum_value_id" +
        " FROM cohort_annotation_definition fromCad" +
        " JOIN cohort_annotation_enum_value fromCaev ON (fromCad.cohort_annotation_definition_id = fromCaev.cohort_annotation_definition_id)" +
        " JOIN participant_cohort_annotations fromPca ON (fromPca.cohort_annotation_definition_id = fromCad.cohort_annotation_definition_id" +
        "                                            AND fromPca.cohort_annotation_enum_value_id = fromCaev.cohort_annotation_enum_value_id" +
        "                                            AND fromCad.cohort_id = :fromCohortId)" +
        " JOIN cohort_annotation_definition toCad ON (toCad.column_name = fromCad.column_name and toCad.cohort_id = :toCohortId)" +
        " JOIN cohort_annotation_enum_value toCaev ON (toCaev.cohort_annotation_definition_id = toCad.cohort_annotation_definition_id" +
        "                                            AND toCaev.enum_order = fromCaev.enum_order)",
        nativeQuery = true)
    void bulkCopyEnumAnnotationsByCohortReviewAndCohort(@Param("fromCohortId") long fromCohortId,
                                                        @Param("toCohortId") long toCohortId,
                                                        @Param("toCohortReviewId") long toCohortReviewId);

    // We use native SQL here as there may be a large number of rows within a
    // given cohort review; this avoids loading them into memory.
    @Modifying
    @Query(
        value = "INSERT INTO participant_cohort_annotations" +
        " (cohort_review_id, cohort_annotation_definition_id, participant_id, cohort_annotation_enum_value_id, annotation_value_string," +
        " annotation_value_integer, annotation_value_date, annotation_value_boolean)" +
        " SELECT :toCohortReviewId, toCad.cohort_annotation_definition_id, participant_id, cohort_annotation_enum_value_id, annotation_value_string," +
        " annotation_value_integer, annotation_value_date, annotation_value_boolean" +
        " FROM cohort_annotation_definition fromCad" +
        " JOIN participant_cohort_annotations fromPca ON (fromCad.cohort_annotation_definition_id = fromPca.cohort_annotation_definition_id" +
        "                                            AND fromCad.cohort_id = :fromCohortId)" +
        " JOIN cohort_annotation_definition toCad ON (fromCad.column_name = toCad.column_name AND toCad.cohort_id = :toCohortId)" +
        " WHERE NOT EXISTS" +
        " (SELECT 'x' FROM cohort_annotation_enum_value fromCaev WHERE fromCad.cohort_annotation_definition_id = fromCaev.cohort_annotation_definition_id)",
        nativeQuery = true)
    void bulkCopyNonEnumAnnotationsByCohortReviewAndCohort(@Param("fromCohortId") long fromCohortId,
                                                           @Param("toCohortId") long toCohortId,
                                                           @Param("toCohortReviewId") long toCohortReviewId);
}

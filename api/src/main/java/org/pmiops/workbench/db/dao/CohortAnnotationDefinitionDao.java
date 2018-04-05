package org.pmiops.workbench.db.dao;


import org.pmiops.workbench.db.model.CohortAnnotationDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CohortAnnotationDefinitionDao extends JpaRepository<CohortAnnotationDefinition, Long> {

    CohortAnnotationDefinition findByCohortIdAndColumnName(@Param("cohortId") long cohortId,
                                                           @Param("ColumnName") String ColumnName);

    List<CohortAnnotationDefinition> findByCohortId(@Param("cohortId") long cohortId);

    CohortAnnotationDefinition findByCohortIdAndCohortAnnotationDefinitionId(@Param("cohortId") long cohortId,
                                                                             @Param("cohortAnnotationDefinitionId") long cohortAnnotationDefinitionId);

    // We use native SQL here as there may be a large number of rows within a
    // given cohort review; this avoids loading them into memory.
    @Modifying
    @Query(
        value="INSERT INTO cohort_annotation_definition" +
        " (cohort_id, column_name, annotation_type)" +
        " SELECT :toCohortId, column_name, annotation_type" +
        " FROM cohort_annotation_definition" +
        " WHERE cohort_id = :fromCohortId",
        nativeQuery=true)
    void bulkCopyCohortAnnotationDefinitionByCohort(@Param("fromCohortId") long fromCohortId,
                                                    @Param("toCohortId") long toCohortId);

    // We use native SQL here as there may be a large number of rows within a
    // given cohort review; this avoids loading them into memory.
    @Modifying
    @Query(
        value="INSERT INTO cohort_annotation_enum_value" +
        " (cohort_annotation_definition_id, name, enum_order)" +
        " SELECT cad2.cohort_annotation_definition_id, name, enum_order" +
        " FROM cohort_annotation_definition cad1" +
        " JOIN cohort_annotation_enum_value caev ON (caev.cohort_annotation_definition_id = cad1.cohort_annotation_definition_id " +
        "                                            AND cad1.cohort_id = :fromCohortId)" +
        " JOIN cohort_annotation_definition cad2 on (cad2.cohort_id = :toCohortId AND cad2.column_name = cad1.column_name)",
        nativeQuery=true)
    /**
     * IMPORTANT NOTE:
     * This method will only bulk copy correctly when called after
     * {@link CohortAnnotationDefinitionDao#bulkCopyCohortAnnotationDefinitionByCohort(long, long)}
     */
    void bulkCopyCohortAnnotationEnumsByCohort(@Param("fromCohortId") long fromCohortId,
                                               @Param("toCohortId") long toCohortId);
}

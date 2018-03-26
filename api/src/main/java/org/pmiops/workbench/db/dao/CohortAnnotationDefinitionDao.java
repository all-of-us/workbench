package org.pmiops.workbench.db.dao;


import org.pmiops.workbench.db.model.CohortAnnotationDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CohortAnnotationDefinitionDao extends JpaRepository<CohortAnnotationDefinition, Long> {

    CohortAnnotationDefinition findByCohortIdAndColumnName(
            @Param("cohortId") long cohortId,
            @Param("ColumnName") String ColumnName);

    List<CohortAnnotationDefinition> findByCohortId(
            @Param("cohortId") long cohortId);

    CohortAnnotationDefinition findByCohortIdAndCohortAnnotationDefinitionId(
            @Param("cohortId") long cohortId,
            @Param("cohortAnnotationDefinitionId") long cohortAnnotationDefinitionId);

    // We use native SQL here as there may be a large number of rows within a
    // given cohort review; this avoids loading them into memory.
    @Modifying
    @Query(
        value = "INSERT INTO cohort_annotation_definition" +
        " (cohort_id, column_name, annotation_type)" +
        " SELECT (:toCohortId), column_name, annotation_type" +
        " FROM cohort_annotation_definition cad" +
        " WHERE cad.cohort_id = (:fromCohortId); SELECT LAST_INSERT_ID();",
        nativeQuery = true)
    int bulkCopyByCohort(
            @Param("fromCohortId") long fromCohortId,
            @Param("toCohortId") long toCohortId);
}

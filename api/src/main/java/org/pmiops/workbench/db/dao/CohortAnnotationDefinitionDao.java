package org.pmiops.workbench.db.dao;


import org.pmiops.workbench.db.model.CohortAnnotationDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface CohortAnnotationDefinitionDao extends JpaRepository<CohortAnnotationDefinition, Long> {

    CohortAnnotationDefinition findByCohortIdAndColumnName(
            @Param("cohortId") long cohortId,
            @Param("ColumnName") String ColumnName);
}

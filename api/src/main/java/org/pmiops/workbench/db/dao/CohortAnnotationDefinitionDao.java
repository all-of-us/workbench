package org.pmiops.workbench.db.dao;


import org.pmiops.workbench.db.model.CohortAnnotationDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CohortAnnotationDefinitionDao extends JpaRepository<CohortAnnotationDefinition, Long> {
}

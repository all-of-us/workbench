package org.pmiops.workbench.cdr.dao;
import org.pmiops.workbench.cdr.model.AchillesAnalysis;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface AchillesAnalysisDao extends CrudRepository<AchillesAnalysis, Long> {
    List<AchillesAnalysis> findAll();
}

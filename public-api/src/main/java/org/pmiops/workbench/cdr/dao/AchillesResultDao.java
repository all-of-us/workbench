package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.AchillesResult;
import org.springframework.data.repository.CrudRepository;

public interface AchillesResultDao extends CrudRepository<AchillesResult, Long> {
    AchillesResult findAchillesResultByAnalysisId(Long analysisId);
}

package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.AchillesResult;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface AchillesResultDao extends CrudRepository<AchillesResult, Long> {

    List<AchillesResult> findAchillesResultByAnalysisIdAndStratum1(Long analysisId, String stratum1);
    AchillesResult findAchillesResultByAnalysisId(Long analysisId);
}

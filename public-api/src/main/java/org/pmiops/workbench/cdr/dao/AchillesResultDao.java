package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.AchillesResult;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface AchillesResultDao extends CrudRepository<AchillesResult, Long> {
    AchillesResult findAchillesResultByAnalysisId(Long analysisId);

    @Query(value = "select ar.count_value from achilles_results ar " +
            "where ar.stratum_1 = ?1 and ar.analysis_id = 3000",
            nativeQuery = true)
    Long getDomainParticipantCount(String conceptId);
}

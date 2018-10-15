package org.pmiops.workbench.cdr.dao;
import org.pmiops.workbench.cdr.model.AchillesResultDist;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface AchillesResultDistDao extends CrudRepository<AchillesResultDist, Long> {
    List<AchillesResultDist> findAll();

    @Query(value = "select * from achilles_results_dist where analysis_id=?1 and stratum_1=?2",nativeQuery=true)
    List<AchillesResultDist> fetchConceptDistResults(Long analysisId, String conceptId);

    @Query(value = "select * from achilles_results_dist where analysis_id in (?1) and stratum_1 in (?2)", nativeQuery = true)
    List<AchillesResultDist> fetchByAnalysisIdsAndConceptIds(List<Long> analysisIds,List<String> conceptIds);

}



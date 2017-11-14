package org.pmiops.workbench.cdr.dao;
import org.pmiops.workbench.cdr.model.AnalysisResult;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnalysisResultDao extends CrudRepository<AnalysisResult, Long> {

    @Query(value = "select * from ACHILLES_results_view r where r.analysis_id = :analysisId and r.stratum_1 = :stratum1",nativeQuery = true)
    List<AnalysisResult> findConceptCountByConceptIdName(@Param("analysisId") Long analysisId, @Param("stratum1") Long stratum1);

    List<AnalysisResult> findByAnalysisIdAndStratum1(Long analysisId, String stratum1);
}

package org.pmiops.workbench.cdr.dao;
import org.pmiops.workbench.cdr.model.AnalysisResult;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnalysisResultDao extends CrudRepository<AnalysisResult, Long> {

    @Query(value = "select r.id, r.analysis_id, r.count_value, r.stratum_1, r.stratum_1_name, r.stratum_2, r.stratum_2_name, r.stratum_3, r.stratum_3_name, r.stratum_4, r.stratum_4_name, r.stratum_5, r.stratum_5_name from cdr.ACHILLES_results_view r where r.analysis_id = :analysisId and r.stratum_1 = :stratum_1",nativeQuery = true)
    List<AnalysisResult> findConceptCountByConceptId(@Param("analysisId") Long analysisId, @Param("stratum_1") String stratum_1);

    List<AnalysisResult> findAnalysisResultsByAnalysisIdAndStratum1(Long analysisId, String stratum1);
    List<AnalysisResult> findAnalysisResultsByAnalysisId(Long analysisId);
   }

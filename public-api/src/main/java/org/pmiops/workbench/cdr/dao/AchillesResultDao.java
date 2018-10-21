package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.AchillesResult;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import java.util.TreeMap;
import java.util.List;

public interface AchillesResultDao extends CrudRepository<AchillesResult, Long> {
    AchillesResult findAchillesResultByAnalysisId(Long analysisId);

    @Query(value = "select distinct id,analysis_id,stratum_1,stratum_2,stratum_3,stratum_4,stratum_5," +
            "count_value,source_count_value from achilles_results where stratum_1='1586134' and analysis_id=3110\n" +
            "order by CAST(stratum_5 AS UNSIGNED) asc;", nativeQuery = true)
    List<AchillesResult> findPPIQuestionOrderBySurveyConceptId(String survey_concept_id);
}

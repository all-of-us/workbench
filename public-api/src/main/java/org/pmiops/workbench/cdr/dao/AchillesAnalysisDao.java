package org.pmiops.workbench.cdr.dao;
import org.pmiops.workbench.cdr.model.AchillesAnalysis;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface AchillesAnalysisDao extends CrudRepository<AchillesAnalysis, Long> {
    List<AchillesAnalysis> findAll();

    @Query(value = "select distinct a from AchillesAnalysis a left join FETCH a.results as r " +
            "where r.stratum1 = ?1 and r.stratum2 in (?2) and a.analysisId in (3110,3111,3112) order by a.analysisId"
    )
    List<AchillesAnalysis> findSurveyAnalysisResults(String survey_concept_id, List<String> question_concept_ids);

    @Query(value = "select a from AchillesAnalysis a left join FETCH a.results as r " +
            "where r.stratum1=?1 and a.analysisId=?2"
    )
    AchillesAnalysis findConceptAnalysisResults(String concept_id,Long analysisId);

}



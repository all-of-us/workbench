package org.pmiops.workbench.cdr.dao;
import org.pmiops.workbench.cdr.model.AchillesAnalysis;
import org.pmiops.workbench.cdr.model.AchillesResult;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AchillesAnalysisDao extends CrudRepository<AchillesAnalysis, Long> {
    List<AchillesAnalysis> findAll();

    AchillesAnalysis findAchillesAnalysisByAnalysisId(long analysisId);
    AchillesAnalysis getByAnalysisId(long analysisId);


    List<AchillesAnalysis> findByResults_Stratum2(String stratum2);
    AchillesAnalysis findAchillesAnalysisByAnalysisIdAndResults_Stratum2(long analysisId, String stratum2);


    @Query(value = "select a from AchillesAnalysis a left join FETCH a.results as r " +
            "where a.analysisId = ?1  and r.stratum2 = ?2"
            )
    AchillesAnalysis findResultsByStratum2(long analysisId, String stratum2);

    @Query(value = "select distinct a from AchillesAnalysis a left join FETCH a.results as r " +
            "where r.stratum1 = ?1 and r.stratum2 = ?2 order by a.analysisId"
    )
    List<AchillesAnalysis> findQuestionAnalysisResults(String survey_concept_id, String question_concept_id);

}



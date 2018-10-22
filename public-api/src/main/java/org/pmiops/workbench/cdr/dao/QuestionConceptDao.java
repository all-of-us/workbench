package org.pmiops.workbench.cdr.dao;
import org.pmiops.workbench.cdr.model.QuestionConcept;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface QuestionConceptDao extends CrudRepository<QuestionConcept, Long> {

    @Query(nativeQuery=true, value="SELECT c.concept_id,c.concept_name,c.domain_id,c.vocabulary_id,c.concept_code,c.count_value,c.prevalence from\n" +
            "concept c \n" +
            "join achilles_results ar on ar.stratum_2=c.concept_id\n" +
            "where ar.stratum_1=?1 and ar.analysis_id=3110\n" +
            "group by c.concept_id,c.concept_name,c.domain_id,c.vocabulary_id,c.concept_code,c.count_value,c.prevalence,ar.stratum_5 \n" +
            "order by CAST(ar.stratum_5 AS UNSIGNED) asc")
    List<QuestionConcept> findSurveyQuestions(String survey_concept_id);
}

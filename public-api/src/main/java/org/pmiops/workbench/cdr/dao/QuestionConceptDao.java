package org.pmiops.workbench.cdr.dao;
import org.pmiops.workbench.cdr.model.QuestionConcept;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface QuestionConceptDao extends CrudRepository<QuestionConcept, Long> {

    @Query(nativeQuery=true, value="SELECT C.concept_id,C.concept_name,C.domain_id,C.vocabulary_id,C.concept_code,C.count_value,C.prevalence from\n" +
            "(SELECT c.*,count(*) as result_count from\n" +
            "concept c join concept_relationship r on c.concept_id = r.concept_id_2\n" +
            "join achilles_results ar on ar.stratum_2=c.concept_id\n" +
            "where r.concept_id_1=?1 and r.relationship_id = 'Module of' and ar.stratum_1=?1\n" +
            "group by c.concept_id,c.concept_name,c.domain_id,c.vocabulary_id,c.concept_code,c.count_value,c.prevalence) as C\n" +
            "where result_count > 0")
    List<QuestionConcept> findSurveyQuestions(long survey_concept_id);
}

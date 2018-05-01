package org.pmiops.workbench.cdr.dao;
import org.pmiops.workbench.cdr.model.QuestionConcept;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionConceptDao extends CrudRepository<QuestionConcept, Long> {

    @Query(nativeQuery=true, value="SELECT concept_id, concept_name, domain_id, vocabulary_id, concept_code, " +
            "count_value, prevalence  from concept c join concept_relationship  r on c.concept_id = r.concept_id_2 " +
            "where r.concept_id_1=?1")
    List<QuestionConcept> findSurveyQuestions(long survey_concept_id);
}

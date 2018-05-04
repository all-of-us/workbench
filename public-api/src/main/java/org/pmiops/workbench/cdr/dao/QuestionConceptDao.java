package org.pmiops.workbench.cdr.dao;
import org.pmiops.workbench.cdr.model.QuestionConcept;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface QuestionConceptDao extends CrudRepository<QuestionConcept, Long> {

    @Query(nativeQuery=true, value="SELECT c.concept_id, c.concept_name, c.domain_id, c.vocabulary_id, c.concept_code, " +
            "c.count_value, c.prevalence  " +
            "from concept c join concept_relationship  r on c.concept_id = r.concept_id_2 " +
            "where r.concept_id_1=?1 and r.relationship_id = 'Module of' ")
    List<QuestionConcept> findSurveyQuestions(long survey_concept_id);
}

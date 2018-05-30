package org.pmiops.workbench.cdr.dao;
import org.pmiops.workbench.cdr.model.QuestionConcept;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface QuestionConceptDao extends CrudRepository<QuestionConcept, Long> {

    @Query(nativeQuery=true, value="SELECT c.concept_id,c.concept_name,c.domain_id,c.vocabulary_id,c.concept_code,c.count_value,c.prevalence from\n" +
            "concept c join concept_relationship r on c.concept_id = r.concept_id_2\n" +
            "join achilles_results ar on ar.stratum_2=c.concept_id\n" +
            "where r.concept_id_1=?1 and r.relationship_id = 'Module of' and ar.analysis_id=3110\n" +
            "group by c.concept_id,c.concept_name,c.domain_id,c.vocabulary_id,c.concept_code,c.count_value,c.prevalence")
    List<QuestionConcept> findSurveyQuestions(long survey_concept_id);
<<<<<<< HEAD
<<<<<<< HEAD

    @Query(nativeQuery=true,value="select c.concept_id,c.concept_name.c.domain_id,c.vocabulary_id,c.concept_code,c.count_value,c.prevalence from \n" +
        "concept where concept_id in (?1)")
    List<QuestionConcept> findConceptAnalysis(List<String> concept_ids);
=======
>>>>>>> 90affba120c094746e44330302b551dda0aebac4
=======
>>>>>>> 90affba120c094746e44330302b551dda0aebac4
}

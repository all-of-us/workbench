package org.pmiops.workbench.cdr.dao;
import org.pmiops.workbench.cdr.model.QuestionConcept;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface QuestionConceptDao extends CrudRepository<QuestionConcept, Long> {

    @Query(nativeQuery=true, value="SELECT c.concept_id,c.concept_name,c.domain_id,c.vocabulary_id,c.concept_code,c.count_value,c.prevalence \n" +
            "from concept c \n" +
            "join survey_question_map sqm on sqm.question_concept_id=c.concept_id\n" +
            "where sqm.survey_concept_id=?1 and sqm.is_main=1 \n" +
            "group by c.concept_id,c.concept_name,c.domain_id,c.vocabulary_id,c.concept_code,c.count_value,c.prevalence,sqm.id \n" +
            "order by sqm.id asc")
    List<QuestionConcept> findSurveyQuestions(String survey_concept_id);

    @Query(nativeQuery = true, value="SELECT c.concept_id,c.concept_name,c.domain_id,c.vocabulary_id,c.concept_code,c.count_value,c.prevalence,\n" +
            "(select count(distinct question_concept_id) from survey_question_map where path=sqm.id and is_main=0) as source_count_value from concept c \n" +
            "join survey_question_map sqm on sqm.question_concept_id=c.concept_id\n" +
            "where sqm.survey_concept_id=?1 and sqm.is_main=0 \n" +
            "and sqm.path = (select id from survey_question_map where question_concept_id=?2) \n" +
            "group by c.concept_id,c.concept_name,c.domain_id,c.vocabulary_id,c.concept_code,c.count_value,c.prevalence,sqm.id \n" +
            "order by sqm.id asc")
    List<QuestionConcept> findSubSurveyQuestions(String survey_concept_id, long question_concept_id);

    @Query(nativeQuery = true,value="select c.concept_id,c.concept_name,c.domain_id,c.vocabulary_id,c.concept_code,c.count_value,c.prevalence from survey_question_map sqm1 join concept c on c.concept_id=sqm1.question_concept_id where is_main=1 and survey_concept_id=?1 " +
            "and (select count(distinct question_concept_id) from survey_question_map where path=sqm1.id and question_concept_id != sqm1.question_concept_id)>0")
    List<QuestionConcept> findSurveyMainQuestionIds(long survey_concept_id);
}

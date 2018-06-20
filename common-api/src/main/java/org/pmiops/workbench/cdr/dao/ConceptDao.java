package org.pmiops.workbench.cdr.dao;

import java.util.List;
import org.pmiops.workbench.cdr.model.Concept;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface ConceptDao extends CrudRepository<Concept, Long> {

    @Query(nativeQuery=true, value="select c.* from concept c join concept_relationship cr on c.concept_id=cr.concept_id_2 " +
            "where cr.concept_id_1=?1 and cr.relationship_id='Maps to' ")
    List<Concept> findStandardConcepts(long concept_id);

    @Query(value = "select c.* " +
            "from concept c " +
            "where (match(c.concept_name) against(:conceptName in boolean mode) or " +
            "match(c.concept_code) against(:conceptName in boolean mode)) and " +
            "c.domain_id=:domain_id and c.standard_concept=:standard_concept " +
            "order by c.count_value desc limit 25;",
            nativeQuery = true)
    List<Concept> findConceptLikeNameAndDomainId(@Param("conceptName") String conceptName,@Param("domain_id") String domain_id,@Param("standard_concept") String standard_concept);

    @Query(value = "select c.* " +
            "from concept c " +
            "where (match(c.concept_name) against(:conceptName in boolean mode) or " +
            "match(c.concept_code) against(:conceptName in boolean mode)) and " +
            "c.domain_id in ('Condition','Observation','Procedure', 'Measurement', 'Drug') and " +
            "c.standard_concept=:standard_concept " +
            "order by c.count_value desc limit 25;",
            nativeQuery = true)
    List<Concept> findConceptLikeName(@Param("conceptName") String conceptName,@Param("standard_concept") String standard_concept);

    @Query(value = "select c.* from concept c " +
            "where c.domain_id in ('Condition','Observation','Procedure', 'Measurement', 'Drug') and " +
            "c.standard_concept=:standard_concept " +
            "order by c.count_value desc limit 25;",
    nativeQuery = true)
    List<Concept> findAllConceptsOrderedByCount(@Param("standard_concept") String standard_concept);

    @Query(value = "select c.* from concept c order by c.count_value desc", nativeQuery = true)
    List<Concept> findConceptsOrderedByCount();


    @Query(value = "select c.* from concept c " +
            "where c.domain_id=:domain_id and " +
            "c.standard_concept=:standard_concept " +
            "order by c.count_value desc limit 25;",
          nativeQuery = true)
    List<Concept> findConceptsByDomainIdOrderedByCount(@Param("domain_id") String domain_id,@Param("standard_concept") String standard_concept);

    @Query(value="select c.* from concept c "+
            "join concept_relationship rel on " +
            "rel.concept_id_1 = c.concept_id and rel.concept_id_2 = :conceptId and " +
            "rel.relationship_id = 'maps to' where c.concept_id != :conceptId and c.source_count_value > :minCount order " +
            "by c.count_value desc",nativeQuery=true)
    List<Concept> findSourceConcepts(@Param("conceptId") long conceptId,@Param("minCount") Integer minCount);

    @Query(value = "select c.* from concept c " +
            "join concept_relationship rel on rel.concept_id_2 = c.concept_id " +
            "and rel.concept_id_1 = :conceptId and rel.relationship_id = 'maps to' " +
            "where c.concept_id != :conceptId order by c.count_value desc",
            nativeQuery = true)
    List<Concept> findConceptsMapsToParents(@Param("conceptId") long conceptId);

    List<Concept> findByConceptName(String conceptName);

    @Query(value = "select c.concept_id, " +
      "c.concept_name, " +
      "c.domain_id, " +
      "c.vocabulary_id, " +
      "c.concept_class_id, " +
      "c.standard_concept, " +
      "c.concept_code, " +
      "c.count_value, " +
      "c.source_count_value, " +
      "c.prevalence " +
      "from concept c " +
      "where c.vocabulary_id in ('Gender', 'Race', 'Ethnicity')",
      nativeQuery = true)
    List<Concept> findGenderRaceEthnicityFromConcept();
}

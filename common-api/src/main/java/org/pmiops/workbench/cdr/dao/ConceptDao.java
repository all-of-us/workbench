package org.pmiops.workbench.cdr.dao;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.pmiops.workbench.cdr.model.DbConcept;
import org.pmiops.workbench.db.model.CommonStorageEnums;
import org.pmiops.workbench.model.Domain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface ConceptDao extends CrudRepository<DbConcept, Long> {

  /**
   * Return standard or all concepts in each vocabulary for the specified domain matching the
   * specified expression, matching concept name, synonym, ID, or code.
   *
   * @param keyword SQL MATCH expression to match concept name or synonym
   * @param conceptTypes can be 'S', 'C' or ''
   * @param domainId domain ID to use when filtering concepts
   * @return per-vocabulary concept counts
   */
  @Query(
      value =
          "select distinct c from DbConcept c "
              + "where (c.countValue > 0 or c.sourceCountValue > 0) "
              + "and matchConcept(c.conceptName, c.conceptCode, c.vocabularyId, c.synonymsStr, ?1) > 0 "
              + "and c.standardConcept IN (?2) "
              + "and c.domainId = ?3")
  Page<DbConcept> findConcepts(
      String keyword, ImmutableList<String> conceptTypes, String domainId, Pageable page);

  /**
   * Return standard or all concepts in each vocabulary for the specified domain matching the
   * specified expression, matching concept name, synonym, ID, or code.
   *
   * @param conceptTypes can be 'S', 'C' or ''
   * @param domainId domain ID to use when filtering concepts
   * @return per-vocabulary concept counts
   */
  @Query(
      value =
          "select distinct c from DbConcept c "
              + "where (c.countValue > 0 or c.sourceCountValue > 0) "
              + "and c.standardConcept IN (?1) "
              + "and c.domainId = ?2")
  Page<DbConcept> findConcepts(ImmutableList<String> conceptTypes, String domainId, Pageable page);

  /**
   * Return PM concepts in each vocabulary for the specified domain matching the specified
   * expression, matching concept name, synonym, ID, or code.
   *
   * @param keyword SQL MATCH expression to match concept name or synonym
   * @param conceptTypes can be 'S', 'C' or ''
   * @return per-vocabulary concept counts
   */
  @Query(
      value =
          "select distinct c from DbConcept c "
              + "where (c.countValue > 0 or c.sourceCountValue > 0) "
              + "and matchConcept(c.conceptName, c.conceptCode, c.vocabularyId, c.synonymsStr, ?1) > 0 "
              + "and c.standardConcept IN (?2) "
              + "and c.domainId = 'Measurement' "
              + "and c.vocabularyId = 'PPI' "
              + "and c.conceptClassId = 'Clinical Observation'")
  Page<DbConcept> findPhysicalMeasurementConcepts(
      String keyword, ImmutableList<String> conceptTypes, Pageable page);

  /**
   * Return PM concepts in each vocabulary for the specified domain matching the specified
   * expression, matching concept name, synonym, ID, or code.
   *
   * @param conceptTypes can be 'S', 'C' or ''
   * @return per-vocabulary concept counts
   */
  @Query(
      value =
          "select distinct c from DbConcept c "
              + "where (c.countValue > 0 or c.sourceCountValue > 0) "
              + "and c.standardConcept IN (?1) "
              + "and c.domainId = 'Measurement' "
              + "and c.vocabularyId = 'PPI' "
              + "and c.conceptClassId = 'Clinical Observation'")
  Page<DbConcept> findPhysicalMeasurementConcepts(
      ImmutableList<String> conceptTypes, Pageable page);

  /**
   * Return any concepts in each vocabulary for the specified domain matching the specified
   * expression, matching concept name, synonym, ID, or code.
   */
  default Page<DbConcept> findConcepts(
      String keyword, ImmutableList<String> conceptTypes, Domain domainId, Pageable pageable) {
    if (Domain.PHYSICALMEASUREMENT.equals(domainId)) {
      return StringUtils.isBlank(keyword)
          ? findPhysicalMeasurementConcepts(conceptTypes, pageable)
          : findPhysicalMeasurementConcepts(keyword, conceptTypes, pageable);
    }
    String toDomainId = CommonStorageEnums.domainToDomainId(domainId);
    return StringUtils.isBlank(keyword)
        ? findConcepts(conceptTypes, toDomainId, pageable)
        : findConcepts(keyword, conceptTypes, toDomainId, pageable);
  }
}

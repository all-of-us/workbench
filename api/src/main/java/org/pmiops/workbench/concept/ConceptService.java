package org.pmiops.workbench.concept;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.dao.DomainInfoDao;
import org.pmiops.workbench.cdr.dao.SurveyModuleDao;
import org.pmiops.workbench.cdr.model.DbConcept;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cdr.model.DbDomainInfo;
import org.pmiops.workbench.cdr.model.DbSurveyModule;
import org.pmiops.workbench.db.model.CommonStorageEnums;
import org.pmiops.workbench.model.Domain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

@Service
public class ConceptService {

  public static class ConceptIds {

    private final List<Long> standardConceptIds;
    private final List<Long> sourceConceptIds;

    public ConceptIds(List<Long> standardConceptIds, List<Long> sourceConceptIds) {
      this.standardConceptIds = standardConceptIds;
      this.sourceConceptIds = sourceConceptIds;
    }

    public List<Long> getStandardConceptIds() {
      return standardConceptIds;
    }

    public List<Long> getSourceConceptIds() {
      return sourceConceptIds;
    }
  }

  @Autowired private ConceptDao conceptDao;
  @Autowired private DomainInfoDao domainInfoDao;
  @Autowired private SurveyModuleDao surveyModuleDao;
  @Autowired private CBCriteriaDao cbCriteriaDao;
  public static final String VOCAB_ID = "PPI";
  public static final String CONCEPT_CLASS_ID = "Clinical Observation";
  public static final String STANDARD_CONCEPTS = "STANDARD_CONCEPTS";
  public static final String STANDARD_CONCEPT_CODE = "S";
  public static final String CLASSIFICATION_CONCEPT_CODE = "C";
  public static final String EMPTY_CONCEPT_CODE = "";
  public static final List<String> STANDARD_CONCEPT_CODES =
      ImmutableList.of(STANDARD_CONCEPT_CODE, CLASSIFICATION_CONCEPT_CODE);
  public static final List<String> ALL_CONCEPT_CODES =
      ImmutableList.of(STANDARD_CONCEPT_CODE, CLASSIFICATION_CONCEPT_CODE, EMPTY_CONCEPT_CODE);

  public ConceptService() {}

  // Used for tests
  public ConceptService(
      ConceptDao conceptDao,
      DomainInfoDao domainInfoDao,
      SurveyModuleDao surveyModuleDao,
      CBCriteriaDao cbCriteriaDao) {
    this.conceptDao = conceptDao;
    this.domainInfoDao = domainInfoDao;
    this.surveyModuleDao = surveyModuleDao;
    this.cbCriteriaDao = cbCriteriaDao;
  }

  public static String modifyMultipleMatchKeyword(String query) {
    // This function modifies the keyword to match all the words if multiple words are present(by
    // adding + before each word to indicate match that matching each word is essential)
    if (query == null || query.trim().isEmpty()) {
      return null;
    }
    String[] keywords = query.split("[,+\\s+]");
    List<String> temp = new ArrayList<>();
    for (String key : keywords) {
      String tempKey;
      // This is to exact match concept codes like 100.0, 507.01. Without this mysql was matching
      // 100*, 507*.
      if (key.contains(".")) {
        tempKey = "\"" + key + "\"";
      } else {
        tempKey = key;
      }
      if (!tempKey.isEmpty()) {
        String toAdd = new String("+" + tempKey);
        if (tempKey.contains("-") && !temp.contains(tempKey)) {
          temp.add(tempKey);
        } else if (tempKey.contains("*") && tempKey.length() > 1) {
          temp.add(toAdd);
        } else {
          if (key.length() < 3) {
            temp.add(key);
          } else {
            temp.add(toAdd);
          }
        }
      }
    }

    StringBuilder query2 = new StringBuilder();
    for (String key : temp) {
      query2.append(key);
    }

    return query2.toString();
  }

  public List<DbDomainInfo> getDomainInfo() {
    return domainInfoDao.findByOrderByDomainId();
  }

  public List<DbDomainInfo> getAllDomainsOrderByDomainId() {
    return domainInfoDao.findByOrderByDomainId();
  }

  public List<DbDomainInfo> getConceptCounts(String matchExp, String standardConceptFilter) {
    return domainInfoDao.findConceptCounts(matchExp, getConceptTypes(standardConceptFilter));
  }

  public DbDomainInfo findPhysicalMeasurementConceptCounts(
      String matchExp, String standardConceptFilter) {
    return domainInfoDao.findPhysicalMeasurementConceptCounts(
        matchExp, getConceptTypes(standardConceptFilter));
  }

  private List<String> getConceptTypes(String standardConceptFilter) {
    return STANDARD_CONCEPTS.equals(standardConceptFilter)
        ? STANDARD_CONCEPT_CODES
        : ALL_CONCEPT_CODES;
  }

  public List<DbSurveyModule> getSurveyInfo() {
    return surveyModuleDao.findByParticipantCountNotOrderByOrderNumberAsc(0L);
  }

  public long countSurveyBySearchTerm(String matchExp) {
    return cbCriteriaDao.countSurveyBySearchTerm(matchExp);
  }

  public long countSurveyByName(String surveyName) {
    return cbCriteriaDao.countSurveyByName(surveyName);
  }

  public long countSurveys() {
    return cbCriteriaDao.countSurveys();
  }

  public Slice<DbConcept> searchConcepts(
      String query, String standardConceptFilter, List<String> domainIds, int limit, int page) {
    final String keyword = modifyMultipleMatchKeyword(query);
    Pageable pageable = new PageRequest(page, limit, new Sort(Direction.DESC, "countValue"));
    List<String> conceptTypes = getConceptTypes(standardConceptFilter);
    if (domainIds.contains(CommonStorageEnums.domainToDomainId(Domain.PHYSICALMEASUREMENT))) {
      ImmutableList domains =
          ImmutableList.of(CommonStorageEnums.domainToDomainId(Domain.MEASUREMENT));
      return StringUtils.isBlank(keyword)
          ? conceptDao.findConcepts(conceptTypes, domains, VOCAB_ID, CONCEPT_CLASS_ID, pageable)
          : conceptDao.findConcepts(
              keyword, conceptTypes, domains, VOCAB_ID, CONCEPT_CLASS_ID, pageable);
    } else {
      return StringUtils.isBlank(keyword)
          ? conceptDao.findConcepts(conceptTypes, domainIds, pageable)
          : conceptDao.findConcepts(keyword, conceptTypes, domainIds, pageable);
    }
  }

  public Slice<DbCriteria> searchSurveys(String query, String surveyName, int limit, int page) {
    final String keyword = modifyMultipleMatchKeyword(query);
    Pageable pageable = new PageRequest(page, limit, new Sort(Direction.ASC, "id"));
    if (StringUtils.isBlank(keyword)) {
      return StringUtils.isBlank(surveyName)
          ? cbCriteriaDao.findSurveys(pageable)
          : cbCriteriaDao.findSurveysByName(surveyName, pageable);
    }
    return cbCriteriaDao.findSurveys(keyword, pageable);
  }

  public ConceptIds classifyConceptIds(Set<Long> conceptIds) {
    ImmutableList.Builder<Long> standardConceptIds = ImmutableList.builder();
    ImmutableList.Builder<Long> sourceConceptIds = ImmutableList.builder();

    Iterable<DbConcept> concepts = conceptDao.findAll(conceptIds);
    for (DbConcept concept : concepts) {
      if (ConceptService.STANDARD_CONCEPT_CODE.equals(concept.getStandardConcept())
          || ConceptService.CLASSIFICATION_CONCEPT_CODE.equals(concept.getStandardConcept())) {
        standardConceptIds.add(concept.getConceptId());
      } else {
        // We may need to handle classification / concept hierarchy here eventually...
        sourceConceptIds.add(concept.getConceptId());
      }
    }
    return new ConceptIds(standardConceptIds.build(), sourceConceptIds.build());
  }
}

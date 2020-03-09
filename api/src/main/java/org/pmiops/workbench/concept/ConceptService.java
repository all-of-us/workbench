package org.pmiops.workbench.concept;

import static org.pmiops.workbench.model.StandardConceptFilter.ALL_CONCEPTS;
import static org.pmiops.workbench.model.StandardConceptFilter.STANDARD_CONCEPTS;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.pmiops.workbench.api.ConceptsController;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.dao.DomainInfoDao;
import org.pmiops.workbench.cdr.dao.SurveyModuleDao;
import org.pmiops.workbench.cdr.model.DbConcept;
import org.pmiops.workbench.cdr.model.DbDomainInfo;
import org.pmiops.workbench.cdr.model.DbSurveyModule;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.Concept;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainCount;
import org.pmiops.workbench.model.StandardConceptFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

@Service
public class ConceptService {

  public static final String STANDARD_CONCEPT_CODE = "S";
  public static final String CLASSIFICATION_CONCEPT_CODE = "C";
  public static final String EMPTY_CONCEPT_CODE = "";
  public static final ImmutableList<String> STANDARD_CONCEPT_CODES =
      ImmutableList.of(STANDARD_CONCEPT_CODE, CLASSIFICATION_CONCEPT_CODE);
  public static final ImmutableList<String> ALL_CONCEPT_CODES =
      ImmutableList.of(STANDARD_CONCEPT_CODE, CLASSIFICATION_CONCEPT_CODE, EMPTY_CONCEPT_CODE);

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

  private ConceptDao conceptDao;
  private DomainInfoDao domainInfoDao;
  private SurveyModuleDao surveyModuleDao;

  public ConceptService() {}

  @Autowired
  public ConceptService(
      ConceptDao conceptDao, DomainInfoDao domainInfoDao, SurveyModuleDao surveyModuleDao) {
    this.conceptDao = conceptDao;
    this.domainInfoDao = domainInfoDao;
    this.surveyModuleDao = surveyModuleDao;
  }

  private static String modifyMultipleMatchKeyword(String query) {
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
        String toAdd = "+" + tempKey;
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

  public Iterable<DbConcept> findAll(Collection<Long> conceptIds) {
    return conceptDao.findAll(conceptIds);
  }

  public List<Concept> findAll(Collection<Long> conceptIds, Ordering<Concept> ordering) {
    Iterable<DbConcept> concepts = conceptDao.findAll(conceptIds);
    return Streams.stream(concepts)
        .map(ConceptsController::toClientConcept)
        .sorted(ordering)
        .collect(Collectors.toList());
  }

  public List<DbDomainInfo> getDomainInfo() {
    return domainInfoDao.findByOrderByDomainId();
  }

  public List<DbSurveyModule> getSurveyInfo() {
    return surveyModuleDao.findByParticipantCountNotOrderByOrderNumberAsc(0L);
  }

  public Slice<DbConcept> searchConcepts(
      String query,
      StandardConceptFilter standardConceptFilter,
      Domain domain,
      int limit,
      int page) {
    final String keyword = modifyMultipleMatchKeyword(query);
    Pageable pageable = new PageRequest(page, limit, new Sort(Direction.DESC, "countValue"));
    ImmutableList<String> conceptTypes = getConceptTypes(standardConceptFilter);
    return conceptDao.findConcepts(keyword, conceptTypes, domain, pageable);
  }

  public List<DbConcept> searchSurveys(String query, String surveyName, int limit, int page) {
    final String keyword = modifyMultipleMatchKeyword(query);
    Pageable pageable = new PageRequest(page, limit, new Sort(Direction.ASC, "id"));
    return conceptDao.findSurveys(keyword, surveyName, pageable);
  }

  public List<DomainCount> countDomains(
      String query, String surveyName, StandardConceptFilter standardConceptFilter) {
    List<DomainCount> domainCountList = new ArrayList<>();
    boolean allConcepts = ALL_CONCEPTS.equals(standardConceptFilter);
    DbDomainInfo pmDomainInfo = null;
    String matchExp = modifyMultipleMatchKeyword(query);
    List<DbDomainInfo> allDbDomainInfos = domainInfoDao.findByOrderByDomainId();
    List<DbDomainInfo> matchingDbDomainInfos =
        matchExp == null ? allDbDomainInfos : new ArrayList<>();
    if (matchingDbDomainInfos.isEmpty()) {
      matchingDbDomainInfos =
          domainInfoDao.findConceptCounts(matchExp, getConceptTypes(standardConceptFilter));
      if (allConcepts) {
        pmDomainInfo =
            domainInfoDao.findPhysicalMeasurementConceptCounts(
                matchExp, getConceptTypes(standardConceptFilter));
      }
    }
    Map<Domain, DbDomainInfo> domainCountMap =
        Maps.uniqueIndex(matchingDbDomainInfos, DbDomainInfo::getDomainEnum);
    // Loop through all domains to populate the results (so we get zeros for domains with no
    // matches.)
    for (DbDomainInfo allDbDomainInfo : allDbDomainInfos) {
      Domain domain = allDbDomainInfo.getDomainEnum();
      DbDomainInfo matchingDbDomainInfo = domainCountMap.get(domain);
      if (domain.equals(Domain.PHYSICALMEASUREMENT) && pmDomainInfo != null) {
        matchingDbDomainInfo = pmDomainInfo;
      }
      domainCountList.add(
          new DomainCount()
              .domain(domain)
              .conceptCount(
                  matchingDbDomainInfo == null
                      ? 0L
                      : (allConcepts
                          ? matchingDbDomainInfo.getAllConceptCount()
                          : matchingDbDomainInfo.getStandardConceptCount()))
              .name(allDbDomainInfo.getName()));
    }
    long conceptCount = 0;
    if (allConcepts) {
      conceptCount = conceptDao.countSurveys(matchExp, surveyName);
    }
    domainCountList.add(
        new DomainCount()
            .domain(Domain.SURVEY)
            .conceptCount(conceptCount)
            .name(DbStorageEnums.domainToDomainId(Domain.SURVEY).concat("s")));
    return domainCountList;
  }

  public ConceptIds classifyConceptIds(Set<Long> conceptIds) {
    ImmutableList.Builder<Long> standardConceptIds = ImmutableList.builder();
    ImmutableList.Builder<Long> sourceConceptIds = ImmutableList.builder();
    StreamSupport.stream(conceptDao.findAll(conceptIds).spliterator(), false)
        .forEach(
            c -> {
              if (STANDARD_CONCEPT_CODES.contains(c.getStandardConcept())) {
                standardConceptIds.add(c.getConceptId());
              } else {
                sourceConceptIds.add(c.getConceptId());
              }
            });
    return new ConceptIds(standardConceptIds.build(), sourceConceptIds.build());
  }

  private ImmutableList<String> getConceptTypes(StandardConceptFilter standardConceptFilter) {
    return STANDARD_CONCEPTS.equals(standardConceptFilter)
        ? STANDARD_CONCEPT_CODES
        : ALL_CONCEPT_CODES;
  }
}

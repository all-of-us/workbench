package org.pmiops.workbench.concept;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConceptService {

  public static final ImmutableList<String> STANDARD_CONCEPT_CODES = ImmutableList.of("S", "C");

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

  public ConceptService() {}

  @Autowired
  public ConceptService(ConceptDao conceptDao) {
    this.conceptDao = conceptDao;
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
}

package org.pmiops.workbench.api;

import com.google.common.base.Strings;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.pmiops.workbench.cdr.dao.ConceptService;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.Concept;
import org.pmiops.workbench.model.ConceptListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConceptsController implements ConceptsApiDelegate {

  private static final Integer DEFAULT_MAX_RESULTS = 20;
  private static final int MAX_MAX_RESULTS = 1000;

  private final ConceptService conceptService;

  private static final Function<org.pmiops.workbench.cdr.model.Concept, Concept> TO_CLIENT_CONCEPT =
      (concept) ->  new Concept()
            .conceptClassId(concept.getConceptClassId())
            .conceptCode(concept.getConceptCode())
            .conceptId(concept.getConceptId())
            .countValue(concept.getCountValue())
            .domainId(concept.getDomainId())
            .prevalence(concept.getPrevalence())
            .standardConcept(ConceptService.STANDARD_CONCEPT_CODE.equals(
                concept.getStandardConcept()))
            .vocabularyId(concept.getVocabularyId());

  @Autowired
  public ConceptsController(ConceptService conceptService) {
    this.conceptService = conceptService;
  }

  @Override
  public ResponseEntity<ConceptListResponse> searchConcepts(String query,
      Boolean standardConcept, String vocabularyId, String domainId, Integer maxResults) {
    if (maxResults == null) {
      maxResults = DEFAULT_MAX_RESULTS;
    } else if (maxResults < 1 || maxResults > MAX_MAX_RESULTS) {
      throw new BadRequestException("Invalid value for maxResults: " + maxResults);
    }
    if (Strings.isNullOrEmpty(query.trim())) {
      throw new BadRequestException("Query must be non-whitespace");
    }
    Slice<org.pmiops.workbench.cdr.model.Concept> concepts =
        conceptService.searchConcepts(query, standardConcept, vocabularyId, domainId, maxResults);
    ConceptListResponse response = new ConceptListResponse();
    response.setItems(concepts.getContent().stream().map(TO_CLIENT_CONCEPT)
        .collect(Collectors.toList()));
    return ResponseEntity.ok(response);
  }
}

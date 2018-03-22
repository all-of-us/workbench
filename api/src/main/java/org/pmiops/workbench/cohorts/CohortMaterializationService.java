package org.pmiops.workbench.cohorts;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantIdAndCohortStatus;
import org.pmiops.workbench.db.model.ParticipantIdAndCohortStatus.Key;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.MaterializeCohortResponse;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.utils.PaginationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CohortMaterializationService {

  @VisibleForTesting
  static final String PERSON_ID = "person_id";

  private static final List<CohortStatus> ALL_STATUSES = Arrays.asList(CohortStatus.values());

  private final BigQueryService bigQueryService;
  private final ParticipantCounter participantCounter;
  private final ParticipantCohortStatusDao participantCohortStatusDao;

  @Autowired
  public CohortMaterializationService(BigQueryService bigQueryService,
      ParticipantCounter participantCounter,
      ParticipantCohortStatusDao participantCohortStatusDao) {
    this.bigQueryService = bigQueryService;
    this.participantCounter = participantCounter;
    this.participantCohortStatusDao = participantCohortStatusDao;
  }

  private Set<Long> getParticipantIdsWithStatus(@Nullable CohortReview cohortReview, List<CohortStatus> statusFilter) {
    if (cohortReview == null) {
      return ImmutableSet.of();
    }
    Set<Long> participantIds = participantCohortStatusDao.findByParticipantKey_CohortReviewIdAndStatusIn(
        cohortReview.getCohortReviewId(), statusFilter)
        .stream()
        .map(ParticipantIdAndCohortStatus::getParticipantKey)
        .map(Key::getParticipantId)
        .collect(Collectors.toSet());
    return participantIds;
  }

  public MaterializeCohortResponse materializeCohort(@Nullable CohortReview cohortReview,
      SearchRequest searchRequest,
      List<CohortStatus> statusFilter, int pageSize, String paginationToken) {
    long offset = 0L;
    // TODO: add CDR version ID here
    Object[] paginationParameters = new Object[] { searchRequest, statusFilter };
    if (paginationToken != null) {
      PaginationToken token = PaginationToken.fromBase64(paginationToken);
      if (token.matchesParameters(paginationParameters)) {
        offset = token.getOffset();
      } else {
        throw new BadRequestException(
            String.format("Use of pagination token %s with new parameter values", paginationToken));
      }
    }
    int limit = pageSize + 1;
    if (statusFilter == null) {
      statusFilter = ALL_STATUSES;
    }

    ParticipantCriteria criteria;
    MaterializeCohortResponse response = new MaterializeCohortResponse();
    if (statusFilter.contains(CohortStatus.NOT_REVIEWED)) {
      Set<Long> participantIdsToExclude;
      if (statusFilter.size() < CohortStatus.values().length) {
        // Find the participant IDs that have statuses which *aren't* in the filter.
        Set<CohortStatus> statusesToExclude =
            Sets.difference(ImmutableSet.copyOf(CohortStatus.values()), ImmutableSet.copyOf(statusFilter));
        participantIdsToExclude = getParticipantIdsWithStatus(cohortReview, ImmutableList.copyOf(statusesToExclude));
      } else {
        participantIdsToExclude = ImmutableSet.of();
      }
      criteria = new ParticipantCriteria(searchRequest, participantIdsToExclude);
    } else {
      Set<Long> participantIds = getParticipantIdsWithStatus(cohortReview, statusFilter);
      if (participantIds.isEmpty()) {
        // There is no cohort review, or no participants matching the status filter;
        // return an empty response.
        return response;
      }
      criteria = new ParticipantCriteria(participantIds);
    }
    QueryResult result = bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(
        participantCounter.buildParticipantIdQuery(criteria, limit, offset)));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);
    int numResults = 0;
    boolean hasMoreResults = false;
    for (List<FieldValue> row : result.iterateAll()) {
      if (numResults == pageSize) {
        hasMoreResults = true;
        break;
      }
      long personId = bigQueryService.getLong(row, rm.get(PERSON_ID));
      Map<String, Object> resultMap = new HashMap<>(1);
      resultMap.put(PERSON_ID, personId);
      response.addResultsItem(resultMap);
      numResults++;
    }
    if (hasMoreResults) {
      // TODO: consider pagination based on cursor / values rather than offset
      PaginationToken token = PaginationToken.of(offset + pageSize, paginationParameters);
      response.setNextPageToken(token.toBase64());
    }
    return response;
  }
}

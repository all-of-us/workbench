package org.pmiops.workbench.cohorts;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.db.model.CdrVersion;
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

  private final BigQueryService bigQueryService;
  private final ParticipantCounter participantCounter;

  @Autowired
  public CohortMaterializationService(BigQueryService bigQueryService,
        ParticipantCounter participantCounter) {
    this.bigQueryService = bigQueryService;
    this.participantCounter = participantCounter;
  }


  public MaterializeCohortResponse materializeCohort(CdrVersion cdrVersion,
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
    // TODO: use CDR version, statusFilter here
    QueryResult result = bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(
        participantCounter.buildParticipantIdQuery(searchRequest, limit, offset)));
    MaterializeCohortResponse response = new MaterializeCohortResponse();
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);
    int numResults = 0;
    boolean hasMoreResults = false;
    for (List<FieldValue> row : result.iterateAll()) {
      long personId = bigQueryService.getLong(row, rm.get(PERSON_ID));
      Map<String, Object> resultMap = new HashMap<>(1);
      resultMap.put(PERSON_ID, personId);
      response.addResultsItem(resultMap);
      numResults++;
      if (numResults == pageSize) {
        hasMoreResults = true;
        break;
      }
    }
    if (hasMoreResults) {
      // TODO: consider pagination based on cursor / values rather than offset
      PaginationToken token = PaginationToken.of(offset + pageSize, paginationParameters);
      response.setNextPageToken(token.toBase64());
    }
    return response;
  }
}

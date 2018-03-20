package org.pmiops.workbench.cohortbuilder;

import com.google.common.collect.ImmutableSet;
import javax.annotation.Nullable;
import org.pmiops.workbench.model.SearchRequest;

/**
 * A class representing participants to use when querying data from a CDR version.
 *
 * Either a cohort definition (with optional blacklist of participants to exclude), or
 * a list of participant IDs to include.
 */
public class ParticipantCriteria {

  private final SearchRequest searchRequest;
  private final ImmutableSet<Long> participantIdsToInclude;
  private final ImmutableSet<Long> participantIdsToExclude;

  public ParticipantCriteria(SearchRequest searchRequest, ImmutableSet<Long> participantIdsToExclude) {
    this.searchRequest = searchRequest;
    this.participantIdsToExclude = participantIdsToExclude;
    this.participantIdsToInclude = null;
  }

  public ParticipantCriteria(ImmutableSet<Long> participantIdsToInclude) {
    this.participantIdsToInclude = participantIdsToInclude;
    this.searchRequest = null;
    this.participantIdsToExclude = null;
  }

  @Nullable
  public SearchRequest getSearchRequest() {
    return searchRequest;
  }

  @Nullable
  public ImmutableSet<Long> getParticipantIdsToInclude() {
    return participantIdsToInclude;
  }

  @Nullable
  public ImmutableSet<Long> getParticipantIdsToExclude() {
    return participantIdsToExclude;
  }
}

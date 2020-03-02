package org.pmiops.workbench.cohortbuilder;

import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.pmiops.workbench.model.AgeType;
import org.pmiops.workbench.model.GenderOrSexType;
import org.pmiops.workbench.model.SearchRequest;

/**
 * A class representing participants to use when querying data from a CDR version.
 *
 * <p>Either a cohort definition (with optional blacklist of participants to exclude), or a list of
 * participant IDs to include.
 *
 * <p>(We could have instead decided to have an interface capable of producing SQL, with different
 * implementations for request + blacklist and whitelist; but this would spread out SQL generation
 * and break the separation between query model and SQL generation code. So instead, this class
 * represents criteria that can be transformed into SQL for any way of selecting participants from
 * BigQuery.)
 */
public class ParticipantCriteria {

  private static final ImmutableSet<Long> NO_PARTICIPANTS_TO_EXCLUDE = ImmutableSet.of();

  private final SearchRequest searchRequest;
  private final Set<Long> participantIdsToInclude;
  private final Set<Long> participantIdsToExclude;
  private final GenderOrSexType genderOrSexType;
  private final AgeType ageType;

  public ParticipantCriteria(SearchRequest searchRequest) {
    this(searchRequest, NO_PARTICIPANTS_TO_EXCLUDE);
  }

  public ParticipantCriteria(
      SearchRequest searchRequest, GenderOrSexType genderOrSexType, AgeType ageType) {
    this.searchRequest = searchRequest;
    this.participantIdsToExclude = NO_PARTICIPANTS_TO_EXCLUDE;
    this.participantIdsToInclude = null;
    this.genderOrSexType = genderOrSexType;
    this.ageType = ageType;
  }

  public ParticipantCriteria(SearchRequest searchRequest, Set<Long> participantIdsToExclude) {
    this.searchRequest = searchRequest;
    this.participantIdsToExclude = participantIdsToExclude;
    this.participantIdsToInclude = null;
    this.genderOrSexType = null;
    this.ageType = null;
  }

  public ParticipantCriteria(Set<Long> participantIdsToInclude) {
    this.participantIdsToInclude = participantIdsToInclude;
    this.searchRequest = null;
    this.participantIdsToExclude = null;
    this.genderOrSexType = null;
    this.ageType = null;
  }

  @Nullable
  public SearchRequest getSearchRequest() {
    return searchRequest;
  }

  @Nullable
  public Set<Long> getParticipantIdsToInclude() {
    return participantIdsToInclude;
  }

  @Nullable
  public Set<Long> getParticipantIdsToExclude() {
    return participantIdsToExclude;
  }

  @Nullable
  public GenderOrSexType getGenderOrSexType() {
    return genderOrSexType;
  }

  @Nullable
  public AgeType getAgeType() {
    return ageType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        searchRequest, participantIdsToExclude, participantIdsToExclude, genderOrSexType, ageType);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ParticipantCriteria)) {
      return false;
    }
    ParticipantCriteria that = (ParticipantCriteria) obj;
    return Objects.equals(this.searchRequest, that.searchRequest)
        && Objects.equals(this.participantIdsToExclude, that.participantIdsToExclude)
        && Objects.equals(this.participantIdsToInclude, that.participantIdsToInclude)
        && Objects.equals(this.genderOrSexType, that.genderOrSexType)
        && Objects.equals(this.ageType, that.ageType);
  }
}

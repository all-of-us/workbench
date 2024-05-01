package org.pmiops.workbench.cohortbuilder;

import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.pmiops.workbench.model.AgeType;
import org.pmiops.workbench.model.CohortDefinition;
import org.pmiops.workbench.model.GenderSexRaceOrEthType;

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

  private final CohortDefinition cohortDefinition;
  private final Set<Long> participantIdsToInclude;
  private final Set<Long> participantIdsToExclude;
  private final GenderSexRaceOrEthType genderSexRaceOrEthType;
  private final AgeType ageType;

  public ParticipantCriteria(CohortDefinition cohortDefinition) {
    this(cohortDefinition, NO_PARTICIPANTS_TO_EXCLUDE);
  }

  public ParticipantCriteria(
      CohortDefinition cohortDefinition,
      GenderSexRaceOrEthType genderSexRaceOrEthType,
      AgeType ageType) {
    this.cohortDefinition = cohortDefinition;
    this.participantIdsToExclude = NO_PARTICIPANTS_TO_EXCLUDE;
    this.participantIdsToInclude = null;
    this.genderSexRaceOrEthType = genderSexRaceOrEthType;
    this.ageType = ageType;
  }

  public ParticipantCriteria(CohortDefinition cohortDefinition, Set<Long> participantIdsToExclude) {
    this.cohortDefinition = cohortDefinition;
    this.participantIdsToExclude = participantIdsToExclude;
    this.participantIdsToInclude = null;
    this.genderSexRaceOrEthType = null;
    this.ageType = null;
  }

  public ParticipantCriteria(Set<Long> participantIdsToInclude) {
    this.participantIdsToInclude = participantIdsToInclude;
    this.cohortDefinition = null;
    this.participantIdsToExclude = null;
    this.genderSexRaceOrEthType = null;
    this.ageType = null;
  }

  @Nullable
  public CohortDefinition getCohortDefinition() {
    return cohortDefinition;
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
  public GenderSexRaceOrEthType getGenderSexRaceOrEthType() {
    return genderSexRaceOrEthType;
  }

  @Nullable
  public AgeType getAgeType() {
    return ageType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        cohortDefinition,
        participantIdsToExclude,
        participantIdsToExclude,
        genderSexRaceOrEthType,
        ageType);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ParticipantCriteria)) {
      return false;
    }
    ParticipantCriteria that = (ParticipantCriteria) obj;
    return Objects.equals(this.cohortDefinition, that.cohortDefinition)
        && Objects.equals(this.participantIdsToExclude, that.participantIdsToExclude)
        && Objects.equals(this.participantIdsToInclude, that.participantIdsToInclude)
        && Objects.equals(this.genderSexRaceOrEthType, that.genderSexRaceOrEthType)
        && Objects.equals(this.ageType, that.ageType);
  }
}

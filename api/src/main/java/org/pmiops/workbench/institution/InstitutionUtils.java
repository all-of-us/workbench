package org.pmiops.workbench.institution;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.InstitutionTierRequirement;
import org.pmiops.workbench.model.TierEmailDomains;

/** Utilities for RW institution related functionalities. */
public class InstitutionUtils {
  private InstitutionUtils() {}

  /**
   * Finds the list of email addresses which from a {@link Institution}'s tier requirement by given
   * tier. Returns empty result if access tier is not found in tier requirement.
   */
  public static Set<String> getEmailAddressesByTierOrEmptySet(
      Institution institution, String accessTierShortName) {
    return institution.getTierEmailAddresses().stream()
        .filter(t -> t.getAccessTierShortName().equals(accessTierShortName))
        .flatMap(
            tea ->
                tea.getEmailAddresses() == null ? Stream.empty() : tea.getEmailAddresses().stream())
        .collect(Collectors.toSet());
  }

  /**
   * Finds the list of email domains which from a {@link Institution}'s tier requirement by given
   * tier. Returns empty result if access tier is not found in tier requirement.
   */
  public static Set<String> getEmailDomainsByTierOrEmptySet(
      Institution institution, String accessTierShortName) {
    Optional<TierEmailDomains> tierEmailDomains =
        institution.getTierEmailDomains().stream()
            .filter(t -> t.getAccessTierShortName().equals(accessTierShortName))
            .findFirst();
    if (!tierEmailDomains.isPresent() || tierEmailDomains.get().getEmailDomains() == null) {
      return new HashSet<>();
    }
    return new HashSet<>(tierEmailDomains.get().getEmailDomains());
  }

  /**
   * Finds {@link InstitutionTierRequirement} which from a {@link Institution}'s tier requirement by
   * given tier.
   */
  public static Optional<InstitutionTierRequirement> getTierRequirement(
      Institution institution, String accessTierShortName) {
    return institution.getTierRequirements().stream()
        .filter(t -> t.getAccessTierShortName().equals(accessTierShortName))
        .findFirst();
  }
}

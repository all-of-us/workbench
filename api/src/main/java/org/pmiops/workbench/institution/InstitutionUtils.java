package org.pmiops.workbench.institution;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.InstitutionTierConfig;

/** Utilities for RW institution related functionalities. */
public class InstitutionUtils {
  private InstitutionUtils() {}

  /** Finds {@link InstitutionTierConfig} which from a {@link Institution}' by given tier. */
  public static Optional<InstitutionTierConfig> getTierConfigByTier(
      Institution institution, String accessTierShortName) {
    if (institution.getTierConfigs() == null) {
      return Optional.empty();
    }
    return institution.getTierConfigs().stream()
        .filter(t -> t.getAccessTierShortName().equals(accessTierShortName))
        .findFirst();
  }

  /**
   * Finds the list of email addresses which from a {@link Institution}'s tier requirement by given
   * tier. Returns empty result if access tier is not found in tier requirement.
   */
  public static Set<String> getEmailAddressesByTierOrEmptySet(
      Institution institution, String accessTierShortName) {
    Optional<InstitutionTierConfig> tierConfig =
        getTierConfigByTier(institution, accessTierShortName);
    if (!tierConfig.isPresent() || tierConfig.get().getEmailAddresses() == null) {
      return new HashSet<>();
    }
    return new HashSet<>(tierConfig.get().getEmailAddresses());
  }

  /**
   * Finds the list of email domains which from a {@link Institution}'s tier requirement by given
   * tier. Returns empty result if access tier is not found in tier requirement.
   */
  public static Set<String> getEmailDomainsByTierOrEmptySet(
      Institution institution, String accessTierShortName) {
    Optional<InstitutionTierConfig> tierConfig =
        getTierConfigByTier(institution, accessTierShortName);
    if (!tierConfig.isPresent() || tierConfig.get().getEmailDomains() == null) {
      return new HashSet<>();
    }
    return new HashSet<>(tierConfig.get().getEmailDomains());
  }
}

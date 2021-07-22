package org.pmiops.workbench.institution;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.InstitutionTierRequirement;
import org.pmiops.workbench.model.TierEmailAddresses;
import org.pmiops.workbench.model.TierEmailDomains;

/** Utilities for RW institution related functionalities. */
public class InstitutionUtils {
    private InstitutionUtils() {}

    /**
     * Finds the list of email addresses which from a {@link Institution}'s tier requirement by
     * given tier. Returns empty result if access tier is not found in tier requirement.
     */
    public static Set<String> getEmailAddressesByTierOrEmptySet(Institution institution, String accessTierShortName) {
        return new HashSet<>(institution.getTierEmailAddresses().stream().filter(t -> t.getAccessTierShortName().equals(accessTierShortName))
            .findFirst().orElse(new TierEmailAddresses().emailAddresses(new ArrayList<>())).getEmailAddresses());
    }

    /**
     * Finds the list of email domains which from a {@link Institution}'s tier requirement by
     * given tier. Returns empty result if access tier is not found in tier requirement.
     */
    public static Set<String> getEmailDomainsByTierOrEmptySet(Institution institution, String accessTierShortName) {
        return new HashSet<>(institution.getTierEmailDomains().stream().filter(t -> t.getAccessTierShortName().equals(accessTierShortName))
            .findFirst().orElse(new TierEmailDomains().emailDomains(new ArrayList<>())).getEmailDomains());
    }

    /**
     * Finds {@link InstitutionTierRequirement} which from a {@link Institution}'s tier requirement
     *  by given tier.
     */
    public static Optional<InstitutionTierRequirement> getTierRequirement(Institution institution, String accessTierShortName) {
        return institution.getTierRequirements().stream().filter(t -> t.getAccessTierShortName().equals(accessTierShortName))
            .findFirst();
    }
}
package org.pmiops.workbench.institution;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.InstitutionTierRequirement;
import org.pmiops.workbench.model.TierEmailAddresses;

/** Utilities for RW institution related functionalities. */
public class InstitutionUtils {
    private InstitutionUtils() {}

    /**
     * Finds the list of email addresses which from a {@link Institution}'s tier requirement by
     * given tier. Throws {@link NotFoundException} if tier not found.
     */
    public static Set<String> getEmailAddressesByTierOrThrow(Institution institution, String accessTierShortName) {
        return new HashSet<>(institution.getTierEmailAddresses().stream().filter(t -> t.getAccessTierShortName().equals(accessTierShortName))
            .findFirst().orElseThrow(() -> new NotFoundException("Access tier " + accessTierShortName + " not found.")).getEmailAddresses());
    }

    /**
     * Finds the list of email domains which from a {@link Institution}'s tier requirement by
     * given tier. Throws {@link NotFoundException} if tier not found.
     */
    public static Set<String> getEmailDomainsByTierOrThrow(Institution institution, String accessTierShortName) {
        return new HashSet<>(institution.getTierEmailDomains().stream().filter(t -> t.getAccessTierShortName().equals(accessTierShortName))
            .findFirst().orElseThrow(() -> new NotFoundException("Access tier " + accessTierShortName + " not found.")).getEmailDomains());
    }

    /**
     * Finds {@link InstitutionTierRequirement} which from a {@link Institution}'s tier requirement
     *  by given tier. Throws {@link NotFoundException} if tier not found.
     */
    public static InstitutionTierRequirement getTierRequirementOrThrow(Institution institution, String accessTierShortName) {
        return institution.getTierRequirements().stream().filter(t -> t.getAccessTierShortName().equals(accessTierShortName))
            .findFirst().orElseThrow(() -> new NotFoundException("Access tier " + accessTierShortName + " not found."));
    }

}
package org.pmiops.workbench.compliance;

import org.pmiops.workbench.moodle.ApiException;
import org.pmiops.workbench.moodle.model.BadgeDetails;

import java.util.List;

public interface ComplianceService {

    /**
     * Get Moodle Id associated with Aou User email id
     * @param email
     * @return Moodle Id
     * @throws ApiException
     */
    Integer getMoodleId(String email) throws ApiException;

    /**
     * Get the list of badges earned by User
     * @param userMoodleId
     * @return list of badges/completed training by user
     * @throws ApiException
     */
    List<BadgeDetails> getUserBadge(int userMoodleId) throws ApiException;
}

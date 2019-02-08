package org.pmiops.workbench.compliance;

import org.pmiops.workbench.moodle.ApiException;
import org.pmiops.workbench.moodle.model.BadgeDetails;

import java.util.List;

public interface ComplianceService {

    int getMoodleId(String email) throws ApiException;

    List<BadgeDetails> getUserBadge(int userId) throws ApiException;
}

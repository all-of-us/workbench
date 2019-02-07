package org.pmiops.workbench.compliance;

import org.pmiops.workbench.moodle.ApiException;

import java.sql.Timestamp;
import java.util.Map;

public interface ComplianceTrainingService {

    int getMoodleId(String email) throws ApiException;

    Map<String, Timestamp> getUserBadge(int userId) throws ApiException;
}

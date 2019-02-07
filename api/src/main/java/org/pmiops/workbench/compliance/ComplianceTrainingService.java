package org.pmiops.workbench.compliance;

import com.google.protobuf.InvalidProtocolBufferException;
import org.pmiops.workbench.moodle.ApiException;

import java.sql.Timestamp;
import java.util.Map;

public interface ComplianceTrainingService {

    int getMoodleId(String email) throws ApiException, InvalidProtocolBufferException;

    Map<String, Timestamp> getUserBadge(int userId) throws ApiException;
}

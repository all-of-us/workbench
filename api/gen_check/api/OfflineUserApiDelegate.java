package org.pmiops.workbench.api;

import org.pmiops.workbench.model.ErrorResponse;

import io.swagger.annotations.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * A delegate to be called by the {@link OfflineUserApiController}}.
 * Should be implemented as a controller but without the {@link org.springframework.stereotype.Controller} annotation.
 * Instead, use spring to autowire this class into the {@link OfflineUserApiController}.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T14:10:50.813-05:00")

public interface OfflineUserApiDelegate {

    /**
     * @see OfflineUserApi#bulkAuditProjectAccess
     */
    ResponseEntity<Void> bulkAuditProjectAccess();

    /**
     * @see OfflineUserApi#bulkSyncComplianceTrainingStatus
     */
    ResponseEntity<Void> bulkSyncComplianceTrainingStatus();

    /**
     * @see OfflineUserApi#bulkSyncEraCommonsStatus
     */
    ResponseEntity<Void> bulkSyncEraCommonsStatus();

    /**
     * @see OfflineUserApi#bulkSyncTwoFactorAuthStatus
     */
    ResponseEntity<Void> bulkSyncTwoFactorAuthStatus();

}
